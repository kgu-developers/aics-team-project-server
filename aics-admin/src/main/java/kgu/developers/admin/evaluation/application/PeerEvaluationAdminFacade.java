package kgu.developers.admin.evaluation.application;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminListResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminTeamDetailResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminTeamSummaryResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationMeetingRecordSummaryResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationMemberResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationRowResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationScoreDetailResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationTeammateAssessmentResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmission;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionStatus;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswer;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswerRepository;
import kgu.developers.domain.evaluation.exception.PeerEvaluationFormNotFoundException;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
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
public class PeerEvaluationAdminFacade {

    private final PeerEvaluationFormRepository formRepository;
    private final PeerEvaluationSubmissionRepository submissionRepository;
    private final PeerEvaluationTeammateAnswerRepository teammateAnswerRepository;
    private final SectionQueryService sectionQueryService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserQueryService userQueryService;
    private final MeetingRecordQueryService meetingRecordQueryService;

    public PeerEvaluationAdminListResponse getPeerEvaluations(Long sectionId, Long formId, String professorId) {
        validateSectionAccess(sectionId, professorId);

        List<Team> teams = teamRepository.findAllBySectionId(sectionId).stream()
            .sorted(Comparator.comparing(Team::getName))
            .toList();
        if (teams.isEmpty()) {
            PeerEvaluationForm form = resolveForm(sectionId, formId);
            return PeerEvaluationAdminListResponse.of(sectionId, form, List.of());
        }

        Set<String> activeStudentUserIds = enrollmentRepository.findAllBySectionId(sectionId).stream()
            .filter(Enrollment::isActiveStudent)
            .map(Enrollment::getUserId)
            .collect(Collectors.toSet());

        List<Long> teamIds = teams.stream().map(Team::getId).toList();
        List<TeamMember> allMembers = teamMemberRepository.findAllByTeamIdIn(teamIds);
        Map<Long, List<TeamMember>> activeMembersByTeamId = allMembers.stream()
            .filter(member -> activeStudentUserIds.contains(member.getUserId()))
            .collect(Collectors.groupingBy(TeamMember::getTeamId));

        Map<Long, Long> meetingRecordCounts = meetingRecordQueryService.countMeetingRecords(teamIds);

        PeerEvaluationForm form = resolveForm(sectionId, formId);
        Map<String, PeerEvaluationSubmission> submissionsByEvaluator = form != null
            ? submissionRepository.findAllByFormId(form.getId()).stream()
                .collect(Collectors.toMap(PeerEvaluationSubmission::getEvaluatorId, Function.identity(), (s1, s2) -> s1))
            : Map.of();

        List<PeerEvaluationAdminTeamSummaryResponse> teamSummaries = teams.stream()
            .map(team -> {
                List<TeamMember> teamMembers = activeMembersByTeamId.getOrDefault(team.getId(), List.of());
                List<PeerEvaluationSubmission> teamSubmissions = teamMembers.stream()
                    .map(m -> submissionsByEvaluator.get(m.getUserId()))
                    .filter(Objects::nonNull)
                    .filter(s -> s.getStatus() == PeerEvaluationSubmissionStatus.SUBMITTED)
                    .toList();

                LocalDateTime lastSubmittedAt = teamSubmissions.stream()
                    .map(PeerEvaluationSubmission::getSubmittedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

                return PeerEvaluationAdminTeamSummaryResponse.builder()
                    .teamId(team.getId())
                    .teamName(team.getName())
                    .submittedCount(teamSubmissions.size())
                    .totalMemberCount(teamMembers.size())
                    .lastSubmittedAt(lastSubmittedAt)
                    .meetingRecordCount(meetingRecordCounts.getOrDefault(team.getId(), 0L))
                    .build();
            })
            .toList();

        return PeerEvaluationAdminListResponse.of(sectionId, form, teamSummaries);
    }

    public PeerEvaluationAdminTeamDetailResponse getTeamPeerEvaluationDetail(
            Long sectionId, Long teamId, Long formId, String professorId) {
        validateSectionAccess(sectionId, professorId);

        Team team = teamRepository.findById(teamId)
            .orElseThrow(TeamNotFoundException::new);
        if (!team.getSectionId().equals(sectionId)) {
            throw new AccessDeniedException("해당 분반에 속한 팀만 조회할 수 있습니다.");
        }

        PeerEvaluationForm form = resolveForm(sectionId, formId);
        if (form == null) {
            throw new PeerEvaluationFormNotFoundException();
        }

        Set<String> activeStudentUserIds = enrollmentRepository.findAllBySectionId(sectionId).stream()
            .filter(Enrollment::isActiveStudent)
            .map(Enrollment::getUserId)
            .collect(Collectors.toSet());

        List<TeamMember> members = teamMemberRepository.findAllByTeamId(teamId).stream()
            .filter(member -> activeStudentUserIds.contains(member.getUserId()))
            .sorted(Comparator.comparing(TeamMember::isLeader).reversed().thenComparing(TeamMember::getUserId))
            .toList();

        List<String> memberUserIds = members.stream().map(TeamMember::getUserId).toList();
        Map<String, User> usersById = userQueryService.getUsersByStudentNumbersIncludingDeleted(memberUserIds).stream()
            .collect(Collectors.toMap(User::getStudentNumber, Function.identity()));

        List<PeerEvaluationSubmission> submissions = submissionRepository.findAllByFormIdAndEvaluatorIdIn(form.getId(), memberUserIds);
        Map<String, PeerEvaluationSubmission> submissionByEvaluatorId = submissions.stream()
            .collect(Collectors.toMap(PeerEvaluationSubmission::getEvaluatorId, Function.identity()));

        List<Long> submittedSubmissionIds = submissions.stream()
            .filter(s -> s.getStatus() == PeerEvaluationSubmissionStatus.SUBMITTED)
            .map(PeerEvaluationSubmission::getId)
            .toList();

        List<PeerEvaluationTeammateAnswer> teammateAnswers = submittedSubmissionIds.isEmpty()
            ? List.of()
            : teammateAnswerRepository.findAllBySubmissionIdIn(submittedSubmissionIds);

        Map<Long, List<PeerEvaluationTeammateAnswer>> answersBySubmissionId = teammateAnswers.stream()
            .collect(Collectors.groupingBy(PeerEvaluationTeammateAnswer::getSubmissionId));

        Map<String, List<Integer>> receivedScoresByTarget = teammateAnswers.stream()
            .filter(a -> a.getContributionPercent() != null)
            .collect(Collectors.groupingBy(
                PeerEvaluationTeammateAnswer::getTargetUserId,
                Collectors.mapping(PeerEvaluationTeammateAnswer::getContributionPercent, Collectors.toList())
            ));

        List<PeerEvaluationMemberResponse> memberResponses = members.stream()
            .map(member -> {
                User user = usersById.get(member.getUserId());
                String name = user != null ? user.getName() : member.getUserId();
                List<Integer> receivedScores = receivedScoresByTarget.getOrDefault(member.getUserId(), List.of());
                Double averageReceived = receivedScores.isEmpty()
                    ? null
                    : Math.round(receivedScores.stream().mapToInt(Integer::intValue).average().orElse(0.0) * 10.0) / 10.0;

                return PeerEvaluationMemberResponse.builder()
                    .userId(member.getUserId())
                    .name(name)
                    .isLeader(member.isLeader())
                    .role(targetRole(member))
                    .averageReceivedScore(averageReceived)
                    .build();
            })
            .toList();

        List<PeerEvaluationRowResponse> rowResponses = members.stream()
            .map(evaluator -> {
                User evaluatorUser = usersById.get(evaluator.getUserId());
                String evaluatorName = evaluatorUser != null ? evaluatorUser.getName() : evaluator.getUserId();
                PeerEvaluationSubmission submission = submissionByEvaluatorId.get(evaluator.getUserId());
                boolean isSubmitted = submission != null && submission.getStatus() == PeerEvaluationSubmissionStatus.SUBMITTED;

                List<PeerEvaluationTeammateAnswer> answers = (isSubmitted && answersBySubmissionId.containsKey(submission.getId()))
                    ? answersBySubmissionId.get(submission.getId())
                    : List.of();
                Map<String, PeerEvaluationTeammateAnswer> answersByTarget = answers.stream()
                    .collect(Collectors.toMap(PeerEvaluationTeammateAnswer::getTargetUserId, Function.identity(), (a1, a2) -> a1));

                List<PeerEvaluationScoreDetailResponse> scores = members.stream()
                    .map(target -> {
                        User targetUser = usersById.get(target.getUserId());
                        String targetName = targetUser != null ? targetUser.getName() : target.getUserId();
                        if (target.getUserId().equals(evaluator.getUserId())) {
                            return PeerEvaluationScoreDetailResponse.self(target.getUserId(), targetName);
                        }
                        Integer score = (isSubmitted && answersByTarget.containsKey(target.getUserId()))
                            ? answersByTarget.get(target.getUserId()).getContributionPercent()
                            : null;
                        return PeerEvaluationScoreDetailResponse.score(target.getUserId(), targetName, score);
                    })
                    .toList();

                List<Integer> givenScores = answers.stream()
                    .map(PeerEvaluationTeammateAnswer::getContributionPercent)
                    .filter(Objects::nonNull)
                    .toList();
                Double averageScore = (isSubmitted && !givenScores.isEmpty())
                    ? Math.round(givenScores.stream().mapToInt(Integer::intValue).average().orElse(0.0) * 10.0) / 10.0
                    : null;

                List<PeerEvaluationTeammateAssessmentResponse> teammateAssessments = members.stream()
                    .filter(target -> !target.getUserId().equals(evaluator.getUserId()))
                    .map(target -> {
                        User targetUser = usersById.get(target.getUserId());
                        String targetName = targetUser != null ? targetUser.getName() : target.getUserId();
                        PeerEvaluationTeammateAnswer answer = answersByTarget.get(target.getUserId());
                        return PeerEvaluationTeammateAssessmentResponse.builder()
                            .targetUserId(target.getUserId())
                            .targetUserName(targetName)
                            .contributionDetail(answer != null ? answer.getContributionDetail() : null)
                            .teammateAssessment(answer != null ? answer.getTeammateAssessment() : null)
                            .build();
                    })
                    .toList();

                return PeerEvaluationRowResponse.builder()
                    .evaluatorId(evaluator.getUserId())
                    .evaluatorName(evaluatorName)
                    .isLeader(evaluator.isLeader())
                    .status(submission != null ? submission.getStatus() : null)
                    .submittedAt(submission != null ? submission.getSubmittedAt() : null)
                    .averageScore(averageScore)
                    .scores(scores)
                    .selfContribution(submission != null ? submission.getSelfContribution() : null)
                    .projectReviewComment(submission != null ? submission.getProjectReviewComment() : null)
                    .reflectionComment(submission != null ? submission.getReflectionComment() : null)
                    .teammateAssessments(teammateAssessments)
                    .build();
            })
            .toList();

        List<MeetingRecord> meetingRecords = meetingRecordQueryService.getMeetingRecords(teamId, null).stream()
            .sorted(Comparator.comparing(MeetingRecord::getMeetingAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MeetingRecord::getId, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        List<PeerEvaluationMeetingRecordSummaryResponse> meetingRecordResponses = meetingRecords.stream()
            .map(PeerEvaluationMeetingRecordSummaryResponse::from)
            .toList();

        return PeerEvaluationAdminTeamDetailResponse.builder()
            .teamId(team.getId())
            .teamName(team.getName())
            .formId(form.getId())
            .closesAt(form.getClosesAt())
            .members(memberResponses)
            .evaluations(rowResponses)
            .meetingRecords(meetingRecordResponses)
            .build();
    }

    private PeerEvaluationForm resolveForm(Long sectionId, Long formId) {
        if (formId != null) {
            PeerEvaluationForm form = formRepository.findById(formId)
                .orElseThrow(PeerEvaluationFormNotFoundException::new);
            if (!form.getSectionId().equals(sectionId)) {
                throw new PeerEvaluationFormNotFoundException();
            }
            return form;
        }
        List<PeerEvaluationForm> forms = formRepository.findAllBySectionIdOrderByIdDesc(sectionId);
        return forms.isEmpty() ? null : forms.get(0);
    }

    private void validateSectionAccess(Long sectionId, String professorId) {
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
            throw new AccessDeniedException("담당 분반의 상호평가 결과만 조회할 수 있습니다.");
        }
    }

    private static String targetRole(TeamMember member) {
        if (member.getProjectRole() != null && !member.getProjectRole().isBlank()) {
            return member.isLeader() ? "팀장 · " + member.getProjectRole() : member.getProjectRole();
        }
        return member.isLeader() ? "팀장" : "팀원";
    }
}
