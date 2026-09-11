package kgu.developers.admin.evaluation.application;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminListResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminRowResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminTeamDetailResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminTeamSummaryResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationCriterionScoreResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationCriterionSimpleResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationMeetingRecordResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.evaluation.application.query.TeamEvaluationCriterionQueryService;
import kgu.developers.domain.evaluation.domain.TeamEvaluation;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationRepository;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScore;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScoreRepository;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PresentationEvaluationAdminFacade {

    private final SectionQueryService sectionQueryService;
    private final MilestoneRepository milestoneRepository;
    private final TeamEvaluationCriterionQueryService criterionQueryService;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final TeamEvaluationRepository teamEvaluationRepository;
    private final TeamEvaluationScoreRepository teamEvaluationScoreRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserQueryService userQueryService;
    private final MeetingRecordQueryService meetingRecordQueryService;

    public PresentationEvaluationAdminListResponse getPresentationEvaluations(
            Long sectionId, Long milestoneId, String professorId) {
        validateSectionAccess(sectionId, professorId);

        Milestone milestone = resolvePresentationMilestone(sectionId, milestoneId);
        List<TeamEvaluationCriterion> criteria = criterionQueryService.getCriteria(sectionId);
        List<PresentationEvaluationCriterionSimpleResponse> criteriaResponses = criteria.stream()
            .map(PresentationEvaluationCriterionSimpleResponse::from)
            .toList();

        if (milestone == null) {
            return PresentationEvaluationAdminListResponse.builder()
                .sectionId(sectionId)
                .milestoneId(null)
                .milestoneTitle(null)
                .closesAt(null)
                .criteria(criteriaResponses)
                .teams(List.of())
                .build();
        }

        List<Team> teams = teamRepository.findAllBySectionId(sectionId).stream()
            .sorted(Comparator.comparing(Team::getName))
            .toList();
        List<Long> teamIds = teams.stream().map(Team::getId).toList();

        Map<Long, String> projectTitles = teamIds.isEmpty() ? Map.of() :
            projectRepository.findAllByTeamIdIn(teamIds).stream()
                .collect(Collectors.toMap(Project::getTeamId, Project::getTitle, (p1, p2) -> p1));

        List<TeamEvaluation> submittedEvaluations = teamEvaluationRepository.findAllByMilestoneId(milestone.getId()).stream()
            .filter(TeamEvaluation::isSubmitted)
            .toList();
        Map<Long, List<TeamEvaluation>> evaluationsByTeamId = submittedEvaluations.stream()
            .collect(Collectors.groupingBy(TeamEvaluation::getRateeTeamId));

        List<Long> evaluationIds = submittedEvaluations.stream().map(TeamEvaluation::getId).toList();
        List<TeamEvaluationScore> allScores = evaluationIds.isEmpty() ? List.of() :
            teamEvaluationScoreRepository.findAllByTeamEvaluationIds(evaluationIds);
        Map<Long, List<TeamEvaluationScore>> scoresByEvaluationId = allScores.stream()
            .collect(Collectors.groupingBy(TeamEvaluationScore::getTeamEvaluationId));

        List<PresentationEvaluationAdminTeamSummaryResponse> teamSummaries = teams.stream()
            .map(team -> {
                List<TeamEvaluation> teamEvaluations = evaluationsByTeamId.getOrDefault(team.getId(), List.of());
                List<PresentationEvaluationCriterionScoreResponse> scoreResponses;
                Double totalScore;

                if (teamEvaluations.isEmpty()) {
                    scoreResponses = criteria.stream()
                        .map(c -> PresentationEvaluationCriterionScoreResponse.of(c.getId(), c.getTitle(), null))
                        .toList();
                    totalScore = null;
                } else {
                    List<TeamEvaluationScore> teamScores = teamEvaluations.stream()
                        .flatMap(e -> scoresByEvaluationId.getOrDefault(e.getId(), List.of()).stream())
                        .toList();
                    Map<Long, List<Integer>> scoresByCriterionId = teamScores.stream()
                        .collect(Collectors.groupingBy(
                            TeamEvaluationScore::getCriterionId,
                            Collectors.mapping(TeamEvaluationScore::getScore, Collectors.toList())
                        ));

                    scoreResponses = criteria.stream()
                        .map(c -> {
                            List<Integer> cScores = scoresByCriterionId.getOrDefault(c.getId(), List.of());
                            Double avg = cScores.isEmpty() ? null :
                                Math.round(cScores.stream().mapToInt(Integer::intValue).average().orElse(0.0) * 10.0) / 10.0;
                            return PresentationEvaluationCriterionScoreResponse.of(c.getId(), c.getTitle(), avg);
                        })
                        .toList();

                    boolean hasAnyScore = scoreResponses.stream().anyMatch(s -> s.score() != null);
                    totalScore = hasAnyScore ?
                        Math.round(scoreResponses.stream()
                            .filter(s -> s.score() != null)
                            .mapToDouble(PresentationEvaluationCriterionScoreResponse::score)
                            .sum() * 10.0) / 10.0 : null;
                }

                return PresentationEvaluationAdminTeamSummaryResponse.builder()
                    .teamId(team.getId())
                    .teamName(team.getName())
                    .projectTitle(projectTitles.get(team.getId()))
                    .evaluationCount(teamEvaluations.size())
                    .scores(scoreResponses)
                    .totalScore(totalScore)
                    .build();
            })
            .toList();

        return PresentationEvaluationAdminListResponse.builder()
            .sectionId(sectionId)
            .milestoneId(milestone.getId())
            .milestoneTitle(milestone.getTitle())
            .closesAt(resolveClosesAt(milestone))
            .criteria(criteriaResponses)
            .teams(teamSummaries)
            .build();
    }

    public PresentationEvaluationAdminTeamDetailResponse getTeamPresentationEvaluationDetail(
            Long sectionId, Long teamId, Long milestoneId, String professorId) {
        validateSectionAccess(sectionId, professorId);

        Team team = teamRepository.findById(teamId)
            .orElseThrow(TeamNotFoundException::new);
        if (!team.getSectionId().equals(sectionId)) {
            throw new AccessDeniedException("해당 분반에 속한 팀만 조회할 수 있습니다.");
        }

        Milestone milestone = resolvePresentationMilestone(sectionId, milestoneId);
        if (milestone == null) {
            throw new MilestoneNotFoundException(milestoneId != null ? milestoneId : 0L);
        }

        List<TeamEvaluationCriterion> criteria = criterionQueryService.getCriteria(sectionId);
        List<PresentationEvaluationCriterionSimpleResponse> criteriaResponses = criteria.stream()
            .map(PresentationEvaluationCriterionSimpleResponse::from)
            .toList();

        String projectTitle = projectRepository.findAllByTeamIdIn(List.of(teamId)).stream()
            .map(Project::getTitle)
            .findFirst()
            .orElse(null);

        Set<String> activeStudentUserIds = enrollmentRepository.findAllBySectionId(sectionId).stream()
            .filter(Enrollment::isActiveStudent)
            .map(Enrollment::getUserId)
            .collect(Collectors.toSet());

        List<Team> allTeams = teamRepository.findAllBySectionId(sectionId);
        List<TeamMember> allMembers = teamMemberRepository.findAllByTeamIdIn(allTeams.stream().map(Team::getId).toList());
        Map<String, Long> teamIdByUserId = allMembers.stream()
            .collect(Collectors.toMap(TeamMember::getUserId, TeamMember::getTeamId, (t1, t2) -> t1));
        Map<Long, String> teamNamesById = allTeams.stream()
            .collect(Collectors.toMap(Team::getId, Team::getName));

        // Candidate evaluators: active students in section who are NOT in the target ratee team
        List<String> evaluatorUserIds = activeStudentUserIds.stream()
            .filter(userId -> {
                Long memberTeamId = teamIdByUserId.get(userId);
                return memberTeamId == null || !memberTeamId.equals(teamId);
            })
            .sorted()
            .toList();

        Map<String, User> usersById = userQueryService.getUsersByStudentNumbersIncludingDeleted(evaluatorUserIds).stream()
            .collect(Collectors.toMap(User::getStudentNumber, Function.identity()));

        List<TeamEvaluation> teamEvaluations = teamEvaluationRepository.findAllByMilestoneIdAndRateeTeamId(milestone.getId(), teamId);
        Map<String, TeamEvaluation> evaluationByRaterId = teamEvaluations.stream()
            .collect(Collectors.toMap(TeamEvaluation::getRaterId, Function.identity(), (e1, e2) -> e1));

        List<Long> evalIds = teamEvaluations.stream().map(TeamEvaluation::getId).toList();
        List<TeamEvaluationScore> scores = evalIds.isEmpty() ? List.of() :
            teamEvaluationScoreRepository.findAllByTeamEvaluationIds(evalIds);
        Map<Long, List<TeamEvaluationScore>> scoresByEvaluationId = scores.stream()
            .collect(Collectors.groupingBy(TeamEvaluationScore::getTeamEvaluationId));

        List<PresentationEvaluationAdminRowResponse> rowResponses = evaluatorUserIds.stream()
            .map(raterId -> {
                User user = usersById.get(raterId);
                String evaluatorName = user != null ? user.getName() : raterId;
                Long raterTeamId = teamIdByUserId.get(raterId);
                String evaluatorTeamName = raterTeamId != null ? teamNamesById.get(raterTeamId) : null;

                TeamEvaluation eval = evaluationByRaterId.get(raterId);
                boolean isSubmitted = eval != null && eval.isSubmitted();

                List<TeamEvaluationScore> evalScores = isSubmitted ?
                    scoresByEvaluationId.getOrDefault(eval.getId(), List.of()) : List.of();
                Map<Long, Integer> scoreByCriterionId = evalScores.stream()
                    .collect(Collectors.toMap(TeamEvaluationScore::getCriterionId, TeamEvaluationScore::getScore, (s1, s2) -> s1));

                List<PresentationEvaluationCriterionScoreResponse> scoreResponses = criteria.stream()
                    .map(c -> {
                        Integer s = isSubmitted ? scoreByCriterionId.get(c.getId()) : null;
                        return PresentationEvaluationCriterionScoreResponse.of(c.getId(), c.getTitle(), s != null ? (double) s : null);
                    })
                    .toList();

                Integer totalScore = isSubmitted && !evalScores.isEmpty() ?
                    evalScores.stream().mapToInt(TeamEvaluationScore::getScore).sum() : null;

                return PresentationEvaluationAdminRowResponse.builder()
                    .evaluatorId(raterId)
                    .evaluatorName(evaluatorName)
                    .teamName(evaluatorTeamName)
                    .isSubmitted(isSubmitted)
                    .submittedAt(eval != null ? eval.getSubmittedAt() : null)
                    .scores(scoreResponses)
                    .totalScore(totalScore)
                    .build();
            })
            .sorted(Comparator.comparing(PresentationEvaluationAdminRowResponse::isSubmitted).reversed()
                .thenComparing(PresentationEvaluationAdminRowResponse::evaluatorId))
            .toList();

        List<MeetingRecord> meetingRecords = meetingRecordQueryService.getMeetingRecords(teamId, null).stream()
            .sorted(Comparator.comparing(MeetingRecord::getMeetingAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MeetingRecord::getId, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        List<PresentationMeetingRecordResponse> meetingRecordResponses = meetingRecords.stream()
            .map(PresentationMeetingRecordResponse::from)
            .toList();

        return PresentationEvaluationAdminTeamDetailResponse.builder()
            .teamId(team.getId())
            .teamName(team.getName())
            .projectTitle(projectTitle)
            .milestoneId(milestone.getId())
            .closesAt(resolveClosesAt(milestone))
            .criteria(criteriaResponses)
            .evaluations(rowResponses)
            .meetingRecords(meetingRecordResponses)
            .build();
    }

    private static LocalDateTime resolveClosesAt(Milestone milestone) {
        if (milestone == null || milestone.getSchedule() == null) {
            return null;
        }
        return milestone.getSchedule().evaluationClosesAt() != null
            ? milestone.getSchedule().evaluationClosesAt()
            : milestone.getSchedule().dueAt();
    }

    private Milestone resolvePresentationMilestone(Long sectionId, Long milestoneId) {
        if (milestoneId != null) {
            Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
            if (!milestone.getSectionId().equals(sectionId) || milestone.getType() != MilestoneType.PRESENTATION) {
                throw new MilestoneNotFoundException(milestoneId);
            }
            return milestone;
        }
        return milestoneRepository.findAllBySectionIdOrderByWeekNumber(sectionId).stream()
            .filter(m -> m.getType() == MilestoneType.PRESENTATION)
            .findFirst()
            .orElse(null);
    }

    private void validateSectionAccess(Long sectionId, String professorId) {
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
            throw new AccessDeniedException("담당 분반의 발표 평가 결과만 조회할 수 있습니다.");
        }
    }
}
