package kgu.developers.admin.evaluation.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.admin.evaluation.presentation.request.ProfessorPresentationEvaluationAdminRequest;
import kgu.developers.admin.evaluation.presentation.request.ProfessorPresentationScoreAdminRequest;
import kgu.developers.admin.evaluation.presentation.response.ProfessorPresentationEvaluationAdminResponse;
import kgu.developers.admin.evaluation.presentation.response.ProfessorPresentationScoreAdminResponse;
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
import kgu.developers.domain.team.domain.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorPresentationEvaluationAdminFacade {
    private final SectionRepository sectionRepository;
    private final MilestoneRepository milestoneRepository;
    private final TeamRepository teamRepository;
    private final TeamEvaluationCriterionRepository criterionRepository;
    private final ProfessorPresentationEvaluationRepository evaluationRepository;
    private final ProfessorPresentationEvaluationScoreRepository scoreRepository;
    private final Clock serviceClock;

    public ProfessorPresentationEvaluationAdminResponse getEvaluation(Long sectionId, Long milestoneId,
            Long teamId, String professorId) {
        validateSectionAccess(sectionId, professorId);
        Milestone milestone = getPresentationMilestone(sectionId, milestoneId, false);
        validateTeam(sectionId, teamId);
        return response(milestone, teamId, LocalDateTime.now(serviceClock));
    }

    @Transactional
    public ProfessorPresentationEvaluationAdminResponse saveEvaluation(Long sectionId, Long milestoneId,
            Long teamId, String professorId, ProfessorPresentationEvaluationAdminRequest request) {
        if (!sectionRepository.lockActiveByIdAndProfessorId(sectionId, professorId)) {
            throw new AccessDeniedException("담당 분반 교수자만 발표 평가를 저장할 수 있습니다.");
        }
        Milestone milestone = getPresentationMilestone(sectionId, milestoneId, true);
        validateTeam(sectionId, teamId);
        LocalDateTime now = LocalDateTime.now(serviceClock);
        if (!isOpen(milestone, now)) {
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
        evaluation.submit(professorId, request.memo(), now);
        Long evaluationId = evaluationRepository.save(evaluation).getId();
        scoreRepository.replaceAll(evaluationId, request.scores().stream()
                .map(score -> ProfessorPresentationEvaluationScore.create(evaluationId,
                        score.criterionId(), score.score(), byId.get(score.criterionId()).getMaxScore()))
                .toList());
        return response(milestone, teamId, now);
    }

    private ProfessorPresentationEvaluationAdminResponse response(Milestone milestone, Long teamId,
            LocalDateTime now) {
        List<TeamEvaluationCriterion> criteria = criterionRepository
                .findAllBySectionIdOrderByDisplayOrder(milestone.getSectionId());
        ProfessorPresentationEvaluation evaluation = evaluationRepository
                .findByMilestoneIdAndTeamId(milestone.getId(), teamId).orElse(null);
        Map<Long, Integer> scores = evaluation == null ? Map.of()
                : scoreRepository.findAllByEvaluationId(evaluation.getId()).stream()
                        .collect(Collectors.toMap(ProfessorPresentationEvaluationScore::criterionId,
                                ProfessorPresentationEvaluationScore::score));
        List<ProfessorPresentationScoreAdminResponse> scoreResponses = criteria.stream()
                .map(criterion -> new ProfessorPresentationScoreAdminResponse(criterion.getId(),
                        criterion.getTitle(), criterion.getMaxScore(), scores.get(criterion.getId())))
                .toList();
        return new ProfessorPresentationEvaluationAdminResponse(milestone.getId(), teamId,
                isOpen(milestone, now), evaluation == null ? null : evaluation.getSubmittedAt(),
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
        teamRepository.findById(teamId)
                .filter(team -> team.getSectionId().equals(sectionId))
                .orElseThrow(() -> new AccessDeniedException("해당 분반의 팀만 평가할 수 있습니다."));
    }

    private boolean isOpen(Milestone milestone, LocalDateTime now) {
        LocalDateTime opensAt = milestone.getSchedule().evaluationOpensAt();
        LocalDateTime closesAt = milestone.getSchedule().evaluationClosesAt();
        return opensAt != null && closesAt != null
                && !now.isBefore(opensAt) && now.isBefore(closesAt);
    }

    private static void validateScores(List<ProfessorPresentationScoreAdminRequest> scores,
            Map<Long, TeamEvaluationCriterion> criteria) {
        if (scores == null || criteria.isEmpty() || scores.stream().anyMatch(java.util.Objects::isNull)) {
            throw new InvalidProfessorPresentationEvaluationException();
        }
        Set<Long> requested = scores.stream().map(ProfessorPresentationScoreAdminRequest::criterionId)
                .collect(Collectors.toSet());
        if (requested.size() != scores.size() || !requested.equals(criteria.keySet())
                || scores.stream().anyMatch(score -> score.score() == null || score.score() < 0
                        || score.score() > criteria.get(score.criterionId()).getMaxScore())) {
            throw new InvalidProfessorPresentationEvaluationException();
        }
    }
}
