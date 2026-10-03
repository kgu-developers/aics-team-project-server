package kgu.developers.admin.evaluation.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.admin.evaluation.presentation.request.ProfessorPresentationEvaluationRequest;
import kgu.developers.admin.evaluation.presentation.request.ProfessorPresentationScoreRequest;
import kgu.developers.admin.evaluation.presentation.response.ProfessorPresentationEvaluationResponse;
import kgu.developers.admin.evaluation.presentation.response.ProfessorPresentationScoreResponse;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluation;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationRepository;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationScore;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationScoreRepository;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterionRepository;
import kgu.developers.domain.evaluation.exception.InvalidProfessorPresentationEvaluationException;
import kgu.developers.domain.evaluation.exception.TeamEvaluationClosedException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorPresentationEvaluationFacade {
    private final SectionRepository sectionRepository;
    private final MilestoneRepository milestoneRepository;
    private final TeamRepository teamRepository;
    private final TeamEvaluationCriterionRepository criterionRepository;
    private final ProfessorPresentationEvaluationRepository evaluationRepository;
    private final ProfessorPresentationEvaluationScoreRepository scoreRepository;
    private final Clock serviceClock;

    public ProfessorPresentationEvaluationResponse getEvaluation(Long sectionId, Long milestoneId,
            Long teamId, String professorId) {
        validateSectionAccess(sectionId, professorId);
        Milestone milestone = getPresentationMilestone(sectionId, milestoneId, false);
        validateTeam(sectionId, teamId);
        return response(milestone, teamId);
    }

    @Transactional
    public ProfessorPresentationEvaluationResponse saveEvaluation(Long sectionId, Long milestoneId,
            Long teamId, String professorId, ProfessorPresentationEvaluationRequest request) {
        if (!sectionRepository.lockActiveByIdAndProfessorId(sectionId, professorId)) {
            throw new AccessDeniedException("담당 분반 교수자만 발표 평가를 저장할 수 있습니다.");
        }
        Milestone milestone = getPresentationMilestone(sectionId, milestoneId, true);
        validateTeam(sectionId, teamId);
        if (!isOpen(milestone)) {
            throw new TeamEvaluationClosedException();
        }
        List<TeamEvaluationCriterion> criteria = criterionRepository
                .findAllBySectionIdOrderByDisplayOrder(sectionId);
        Map<Long, TeamEvaluationCriterion> byId = criteria.stream()
                .collect(Collectors.toMap(TeamEvaluationCriterion::getId, Function.identity()));
        validateScores(request.scores(), byId);

        ProfessorPresentationEvaluation evaluation = evaluationRepository
                .findByMilestoneIdAndTeamId(milestoneId, teamId)
                .orElseGet(() -> ProfessorPresentationEvaluation.create(milestoneId, teamId));
        evaluation.submit(professorId, request.memo(), LocalDateTime.now(serviceClock));
        Long evaluationId = evaluationRepository.save(evaluation).getId();
        scoreRepository.replaceAll(evaluationId, request.scores().stream()
                .map(score -> ProfessorPresentationEvaluationScore.create(evaluationId,
                        score.criterionId(), score.score(), byId.get(score.criterionId()).getMaxScore()))
                .toList());
        return response(milestone, teamId);
    }

    private ProfessorPresentationEvaluationResponse response(Milestone milestone, Long teamId) {
        List<TeamEvaluationCriterion> criteria = criterionRepository
                .findAllBySectionIdOrderByDisplayOrder(milestone.getSectionId());
        ProfessorPresentationEvaluation evaluation = evaluationRepository
                .findByMilestoneIdAndTeamId(milestone.getId(), teamId).orElse(null);
        Map<Long, Integer> scores = evaluation == null ? Map.of()
                : scoreRepository.findAllByEvaluationId(evaluation.getId()).stream()
                        .collect(Collectors.toMap(ProfessorPresentationEvaluationScore::criterionId,
                                ProfessorPresentationEvaluationScore::score));
        List<ProfessorPresentationScoreResponse> scoreResponses = criteria.stream()
                .map(criterion -> new ProfessorPresentationScoreResponse(criterion.getId(),
                        criterion.getTitle(), criterion.getMaxScore(), scores.get(criterion.getId())))
                .toList();
        return new ProfessorPresentationEvaluationResponse(milestone.getId(), teamId,
                isOpen(milestone), evaluation == null ? null : evaluation.getSubmittedAt(),
                scoreResponses, evaluation == null ? null : evaluation.getMemo());
    }

    private void validateSectionAccess(Long sectionId, String professorId) {
        if (!sectionRepository.existsActiveByIdAndProfessorId(sectionId, professorId)) {
            throw new AccessDeniedException("담당 분반 교수자만 발표 평가를 조회할 수 있습니다.");
        }
    }

    private Milestone getPresentationMilestone(Long sectionId, Long milestoneId, boolean forUpdate) {
        Milestone milestone = (forUpdate
                ? milestoneRepository.findByIdAndSectionIdForUpdate(milestoneId, sectionId)
                : milestoneRepository.findByIdAndSectionId(milestoneId, sectionId))
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        if (milestone.getType() != MilestoneType.PRESENTATION
                || milestone.getStatus() == MilestoneStatus.DRAFT) {
            throw new AccessDeniedException("공개된 발표 마일스톤만 평가할 수 있습니다.");
        }
        return milestone;
    }

    private void validateTeam(Long sectionId, Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);
        if (!team.getSectionId().equals(sectionId)) {
            throw new AccessDeniedException("해당 분반의 팀만 평가할 수 있습니다.");
        }
    }

    private boolean isOpen(Milestone milestone) {
        LocalDateTime opensAt = milestone.getSchedule().evaluationOpensAt();
        LocalDateTime closesAt = milestone.getSchedule().evaluationClosesAt();
        LocalDateTime now = LocalDateTime.now(serviceClock);
        return opensAt != null && closesAt != null
                && !now.isBefore(opensAt) && now.isBefore(closesAt);
    }

    private static void validateScores(List<ProfessorPresentationScoreRequest> scores,
            Map<Long, TeamEvaluationCriterion> criteria) {
        if (scores == null || criteria.isEmpty() || scores.stream().anyMatch(java.util.Objects::isNull)) {
            throw new InvalidProfessorPresentationEvaluationException();
        }
        Set<Long> requested = scores.stream().map(ProfessorPresentationScoreRequest::criterionId)
                .collect(Collectors.toSet());
        if (requested.size() != scores.size() || !requested.equals(criteria.keySet())
                || scores.stream().anyMatch(score -> score.score() == null || score.score() < 0
                        || score.score() > criteria.get(score.criterionId()).getMaxScore())) {
            throw new InvalidProfessorPresentationEvaluationException();
        }
    }
}
