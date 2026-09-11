package kgu.developers.api.evaluation.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.api.evaluation.presentation.TeamEvaluationWindowState;
import kgu.developers.api.evaluation.presentation.request.TeamEvaluationScoreRequest;
import kgu.developers.api.evaluation.presentation.request.TeamEvaluationSubmitRequest;
import kgu.developers.api.evaluation.presentation.response.MyTeamEvaluationsResponse;
import kgu.developers.api.evaluation.presentation.response.TeamEvaluationCriterionResponse;
import kgu.developers.api.evaluation.presentation.response.TeamEvaluationResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.evaluation.domain.TeamEvaluation;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterionRepository;
import kgu.developers.domain.evaluation.domain.TeamEvaluationRepository;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScore;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScoreRepository;
import kgu.developers.domain.evaluation.exception.InvalidTeamEvaluationResponseException;
import kgu.developers.domain.evaluation.exception.TeamEvaluationClosedException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.submission.domain.SubmissionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamEvaluationFacade {
    private final MilestoneRepository milestoneRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserQueryService userQueryService;
    private final TeamEvaluationCriterionRepository criterionRepository;
    private final TeamEvaluationRepository evaluationRepository;
    private final TeamEvaluationScoreRepository scoreRepository;
    private final SubmissionRepository submissionRepository;

    public MyTeamEvaluationsResponse getMyEvaluations(Long milestoneId, String userId) {
        AccessContext context = accessContext(milestoneId, userId, false);
        List<TeamEvaluationCriterion> criteria = criterionRepository
                .findAllBySectionIdOrderByDisplayOrder(context.milestone().getSectionId());
        List<TeamEvaluation> evaluations = evaluationRepository
                .findAllByMilestoneIdAndRaterId(milestoneId, userId);
        Map<Long, List<TeamEvaluationScore>> scoresByEvaluationId = evaluations.isEmpty()
                ? Map.of()
                : scoreRepository.findAllByTeamEvaluationIds(evaluations.stream()
                                .map(TeamEvaluation::getId)
                                .toList()).stream()
                        .collect(Collectors.groupingBy(TeamEvaluationScore::getTeamEvaluationId));
        List<TeamEvaluationResponse> evaluationResponses = evaluations.stream()
                .map(evaluation -> TeamEvaluationResponse.of(
                        evaluation,
                        scoresByEvaluationId.getOrDefault(evaluation.getId(), List.of())
                ))
                .toList();

        return new MyTeamEvaluationsResponse(
                milestoneId,
                windowState(context.milestone(), LocalDateTime.now()),
                context.milestone().getSchedule().evaluationOpensAt(),
                context.milestone().getSchedule().evaluationClosesAt(),
                criteria.stream().map(TeamEvaluationCriterionResponse::from).toList(),
                evaluationResponses
        );
    }

    @Transactional
    public TeamEvaluationResponse submit(
            Long milestoneId,
            Long teamId,
            String userId,
            TeamEvaluationSubmitRequest request
    ) {
        AccessContext context = accessContext(milestoneId, userId, true);
        Team targetTeam = teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);
        if (!targetTeam.getSectionId().equals(context.milestone().getSectionId())) {
            throw new AccessDeniedException("같은 분반의 팀만 평가할 수 있습니다.");
        }
        if (targetTeam.getId().equals(context.membership().getTeamId())) {
            throw new AccessDeniedException("본인 팀은 발표 평가 대상이 아닙니다.");
        }
        if (submissionRepository.findByTeamIdAndMilestoneId(teamId, milestoneId).isEmpty()) {
            throw new AccessDeniedException("해당 발표 마일스톤의 제출 대상 팀만 평가할 수 있습니다.");
        }

        List<TeamEvaluationCriterion> criteria = criterionRepository
                .findAllBySectionIdOrderByDisplayOrder(context.milestone().getSectionId());
        Map<Long, TeamEvaluationCriterion> criteriaById = criteria.stream()
                .collect(Collectors.toMap(TeamEvaluationCriterion::getId, Function.identity()));
        validateScores(request.scores(), criteriaById);

        TeamEvaluation evaluation = evaluationRepository
                .findByMilestoneIdAndRaterIdAndRateeTeamId(milestoneId, userId, teamId)
                .orElseGet(() -> TeamEvaluation.create(milestoneId, userId, teamId));
        TeamEvaluation savedEvaluation = evaluation.getId() == null
                ? evaluationRepository.save(evaluation)
                : evaluation;
        Long evaluationId = savedEvaluation.getId();

        scoreRepository.deleteAllByTeamEvaluationId(evaluationId);
        List<TeamEvaluationScore> savedScores = scoreRepository.saveAll(request.scores().stream()
                .map(score -> TeamEvaluationScore.create(
                        evaluationId,
                        score.criterionId(),
                        score.score(),
                        criteriaById.get(score.criterionId()).getMaxScore()
                ))
                .toList());
        savedEvaluation.submit(LocalDateTime.now());
        savedEvaluation = evaluationRepository.save(savedEvaluation);
        return TeamEvaluationResponse.of(savedEvaluation, savedScores);
    }

    private AccessContext accessContext(Long milestoneId, String userId, boolean forUpdate) {
        User requester = userQueryService.getUserByStudentNumber(userId);
        if (requester.getGlobalRole() != UserGlobalRole.USER) {
            throw new AccessDeniedException("일반 사용자 중 활성 학생만 발표 평가에 접근할 수 있습니다.");
        }
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        if (milestone.getType() != MilestoneType.PRESENTATION || milestone.getStatus() == MilestoneStatus.DRAFT) {
            throw new AccessDeniedException("공개된 발표 마일스톤만 평가할 수 있습니다.");
        }
        Enrollment enrollment = (forUpdate
                ? enrollmentRepository.findBySectionIdAndUserIdForUpdate(milestone.getSectionId(), userId)
                : enrollmentRepository.findBySectionIdAndUserId(milestone.getSectionId(), userId))
                .filter(Enrollment::isActiveStudent)
                .orElseThrow(() -> new AccessDeniedException("해당 분반의 활성 학생만 발표 평가에 접근할 수 있습니다."));
        TeamMember membership = teamMemberRepository
                .findActiveBySectionIdAndUserId(enrollment.getSectionId(), userId)
                .orElseThrow(() -> new AccessDeniedException("팀에 소속된 학생만 발표 평가에 접근할 수 있습니다."));
        if (forUpdate && windowState(milestone, LocalDateTime.now()) != TeamEvaluationWindowState.OPEN) {
            throw new TeamEvaluationClosedException();
        }
        return new AccessContext(milestone, membership);
    }

    private static void validateScores(
            List<TeamEvaluationScoreRequest> scores,
            Map<Long, TeamEvaluationCriterion> criteriaById
    ) {
        if (criteriaById.isEmpty() || scores.stream().anyMatch(java.util.Objects::isNull)) {
            throw new InvalidTeamEvaluationResponseException();
        }
        Set<Long> requestedCriterionIds = scores.stream()
                .map(TeamEvaluationScoreRequest::criterionId)
                .collect(Collectors.toSet());
        boolean invalidScore = scores.stream().anyMatch(score -> {
            TeamEvaluationCriterion criterion = criteriaById.get(score.criterionId());
            return criterion == null || score.score() == null
                    || score.score() < 0 || score.score() > criterion.getMaxScore();
        });
        if (invalidScore || requestedCriterionIds.size() != scores.size()
                || !requestedCriterionIds.equals(criteriaById.keySet())) {
            throw new InvalidTeamEvaluationResponseException();
        }
    }

    private static TeamEvaluationWindowState windowState(Milestone milestone, LocalDateTime now) {
        LocalDateTime opensAt = milestone.getSchedule().evaluationOpensAt();
        LocalDateTime closesAt = milestone.getSchedule().evaluationClosesAt();
        if (opensAt == null || closesAt == null) {
            return TeamEvaluationWindowState.UNAVAILABLE;
        }
        if (now.isBefore(opensAt)) {
            return TeamEvaluationWindowState.UPCOMING;
        }
        if (!now.isBefore(closesAt)) {
            return TeamEvaluationWindowState.CLOSED;
        }
        return TeamEvaluationWindowState.OPEN;
    }

    private record AccessContext(Milestone milestone, TeamMember membership) {
    }
}
