package kgu.developers.api.evaluation.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.api.evaluation.presentation.request.PeerEvaluationAnswerRequest;
import kgu.developers.api.evaluation.presentation.request.PeerEvaluationResponseRequest;
import kgu.developers.api.evaluation.presentation.PeerEvaluationAnswerKind;
import kgu.developers.api.evaluation.presentation.response.EvaluationContextResponse;
import kgu.developers.api.evaluation.presentation.response.MyPeerEvaluationResponse;
import kgu.developers.api.evaluation.presentation.response.PeerEvaluationAnswerResponse;
import kgu.developers.api.evaluation.presentation.response.PeerEvaluationTargetResponse;
import kgu.developers.api.evaluation.presentation.response.PeerEvaluationTargetsResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmission;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswer;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswerRepository;
import kgu.developers.domain.evaluation.exception.InvalidPeerEvaluationResponseException;
import kgu.developers.domain.evaluation.exception.PeerEvaluationClosedException;
import kgu.developers.domain.evaluation.exception.PeerEvaluationFormNotFoundException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
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
public class PeerEvaluationFacade {
    private final PeerEvaluationFormRepository formRepository;
    private final MilestoneRepository milestoneRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserQueryService userQueryService;
    private final PeerEvaluationSubmissionRepository submissionRepository;
    private final PeerEvaluationTeammateAnswerRepository teammateAnswerRepository;

    public EvaluationContextResponse getContext(Long sectionId, String userId) {
        User requester = userQueryService.getUserByStudentNumber(userId);
        if (requester.getGlobalRole() != UserGlobalRole.USER) {
            throw new AccessDeniedException("일반 사용자 중 활성 학생만 평가 정보를 조회할 수 있습니다.");
        }
        enrollmentRepository.findBySectionIdAndUserId(sectionId, userId)
            .filter(Enrollment::isActiveStudent)
            .orElseThrow(() -> new AccessDeniedException("해당 분반의 활성 학생만 평가 정보를 조회할 수 있습니다."));

        List<Milestone> visibleMilestones = milestoneRepository.findAllBySectionIdOrderByWeekNumber(sectionId).stream()
            .filter(milestone -> milestone.getStatus() != MilestoneStatus.DRAFT)
            .toList();
        Long presentationMilestoneId = visibleMilestones.stream()
            .filter(milestone -> milestone.getType() == MilestoneType.PRESENTATION)
            .map(Milestone::getId)
            .findFirst()
            .orElse(null);
        Set<Long> peerEvaluationMilestoneIds = visibleMilestones.stream()
            .filter(milestone -> milestone.getType() == MilestoneType.PEER_EVALUATION)
            .map(Milestone::getId)
            .collect(Collectors.toSet());
        Long peerEvaluationFormId = formRepository.findAllBySectionIdOrderByIdDesc(sectionId).stream()
            .filter(form -> peerEvaluationMilestoneIds.contains(form.getMilestoneId()))
            .map(PeerEvaluationForm::getId)
            .findFirst()
            .orElse(null);

        return EvaluationContextResponse.of(presentationMilestoneId, peerEvaluationFormId);
    }

    public PeerEvaluationTargetsResponse getTargets(Long formId, String userId) {
        AccessContext context = accessContext(formId, userId, false);
        PeerEvaluationForm form = context.form();
        List<TeamMember> members = context.targets();
        Map<String, User> usersById = usersById(members);
        List<PeerEvaluationTargetResponse> targets = members.stream()
            .filter(member -> usersById.containsKey(member.getUserId()))
            .map(member -> PeerEvaluationTargetResponse.builder()
                .userId(member.getUserId())
                .name(usersById.get(member.getUserId()).getName())
                .role(targetRole(member))
                .build())
            .toList();
        Milestone milestone = milestoneRepository.findById(form.getMilestoneId())
            .orElseThrow(() -> new MilestoneNotFoundException(form.getMilestoneId()));
        Window window = Window.at(form, LocalDateTime.now());
        return PeerEvaluationTargetsResponse.builder()
            .formId(form.getId())
            .title(milestone.getTitle())
            .windowState(window.state())
            .windowMessage(window.message())
            .targets(targets)
            .myResponse(submissionRepository.findByFormIdAndEvaluatorId(formId, userId)
                .map(this::response)
                .orElse(null))
            .build();
    }

    @Transactional
    public MyPeerEvaluationResponse submitResponse(
        Long formId,
        String userId,
        PeerEvaluationResponseRequest request
    ) {
        AccessContext context = accessContext(formId, userId, true);
        if (request.answers().stream().anyMatch(java.util.Objects::isNull)
            || exceedsLimit(request.selfContribution())
            || exceedsLimit(request.projectReviewComment())) {
            throw new InvalidPeerEvaluationResponseException();
        }
        List<PeerEvaluationAnswerRequest> teammateRequests = request.answers().stream()
            .filter(answer -> answer.kind() == PeerEvaluationAnswerKind.TEAMMATE_CONTRIBUTION)
            .toList();
        List<PeerEvaluationAnswerRequest> reflections = request.answers().stream()
            .filter(answer -> answer.kind() == PeerEvaluationAnswerKind.REFLECTION)
            .toList();
        if (teammateRequests.size() + reflections.size() != request.answers().size()
            || reflections.size() > 1) {
            throw new InvalidPeerEvaluationResponseException();
        }

        Set<String> validTargetIds = context.targets().stream().map(TeamMember::getUserId).collect(Collectors.toSet());
        Set<String> requestedTargetIds = teammateRequests.stream()
            .map(PeerEvaluationAnswerRequest::targetUserId)
            .collect(Collectors.toSet());
        boolean hasInvalidTarget = teammateRequests.stream().anyMatch(answer ->
            answer.targetUserId() == null || !validTargetIds.contains(answer.targetUserId())
                || (answer.contributionPercent() != null
                    && (answer.contributionPercent() < 0 || answer.contributionPercent() > 100))
                || exceedsLimit(answer.contributionDetail()) || exceedsLimit(answer.teammateAssessment())
        );
        boolean hasInvalidReflection = reflections.stream().anyMatch(answer -> exceedsLimit(answer.comment()));
        if (hasInvalidTarget || hasInvalidReflection || requestedTargetIds.size() != teammateRequests.size()) {
            throw new InvalidPeerEvaluationResponseException();
        }
        if (request.submit()) {
            validateFinalResponse(request, teammateRequests, reflections, validTargetIds, requestedTargetIds);
        }

        PeerEvaluationSubmission submission = submissionRepository.findByFormIdAndEvaluatorId(formId, userId)
            .orElseGet(() -> PeerEvaluationSubmission.create(formId, userId));
        submission.update(
            request.selfContribution(),
            request.projectReviewComment(),
            reflections.isEmpty() ? "" : reflections.get(0).comment(),
            request.submit(),
            LocalDateTime.now()
        );
        PeerEvaluationSubmission saved = submissionRepository.save(submission);
        teammateAnswerRepository.deleteAllBySubmissionId(saved.getId());
        List<PeerEvaluationTeammateAnswer> savedAnswers = teammateAnswerRepository.saveAll(teammateRequests.stream()
            .map(answer -> PeerEvaluationTeammateAnswer.create(
                saved.getId(), answer.targetUserId(), answer.contributionPercent(),
                answer.contributionDetail(), answer.teammateAssessment()
            ))
            .toList());
        return response(saved, savedAnswers);
    }

    private AccessContext accessContext(Long formId, String userId, boolean forUpdate) {
        User requester = userQueryService.getUserByStudentNumber(userId);
        if (requester.getGlobalRole() != UserGlobalRole.USER) {
            throw new AccessDeniedException("일반 사용자 중 활성 학생만 상호평가에 접근할 수 있습니다.");
        }

        PeerEvaluationForm form = formRepository.findById(formId)
            .orElseThrow(PeerEvaluationFormNotFoundException::new);
        Enrollment enrollment = (forUpdate
            ? enrollmentRepository.findBySectionIdAndUserIdForUpdate(form.getSectionId(), userId)
            : enrollmentRepository.findBySectionIdAndUserId(form.getSectionId(), userId))
            .filter(Enrollment::isActiveStudent)
            .orElseThrow(() -> new AccessDeniedException("해당 분반의 활성 학생만 상호평가에 접근할 수 있습니다."));
        TeamMember requesterMembership = teamMemberRepository
            .findActiveBySectionIdAndUserId(enrollment.getSectionId(), userId)
            .orElseThrow(() -> new AccessDeniedException("팀에 소속된 학생만 상호평가에 접근할 수 있습니다."));

        List<TeamMember> members = teamMemberRepository.findAllByTeamId(requesterMembership.getTeamId()).stream()
            .filter(member -> !member.getUserId().equals(userId))
            .filter(member -> enrollmentRepository.findBySectionIdAndUserId(form.getSectionId(), member.getUserId())
                .map(Enrollment::isActiveStudent)
                .orElse(false))
            .toList();
        Window window = Window.at(form, LocalDateTime.now());
        if (forUpdate && !"OPEN".equals(window.state())) {
            throw new PeerEvaluationClosedException();
        }
        return new AccessContext(form, members);
    }

    private Map<String, User> usersById(List<TeamMember> members) {
        return userQueryService.getUsersByStudentNumbers(members.stream().map(TeamMember::getUserId).toList())
            .stream()
            .collect(Collectors.toMap(User::getStudentNumber, Function.identity()));
    }

    private void validateFinalResponse(
        PeerEvaluationResponseRequest request,
        List<PeerEvaluationAnswerRequest> teammateRequests,
        List<PeerEvaluationAnswerRequest> reflections,
        Set<String> validTargetIds,
        Set<String> requestedTargetIds
    ) {
        if (teammateRequests.stream().anyMatch(answer -> answer.contributionPercent() == null)) {
            throw new InvalidPeerEvaluationResponseException();
        }
        int contributionSum = teammateRequests.stream().mapToInt(PeerEvaluationAnswerRequest::contributionPercent).sum();
        boolean blankNarrative = isBlank(request.selfContribution()) || isBlank(request.projectReviewComment())
            || reflections.size() != 1 || isBlank(reflections.get(0).comment())
            || teammateRequests.stream().anyMatch(answer ->
                isBlank(answer.contributionDetail()) || isBlank(answer.teammateAssessment()));
        if (!requestedTargetIds.equals(validTargetIds) || contributionSum != 100 || blankNarrative) {
            throw new InvalidPeerEvaluationResponseException();
        }
    }

    private MyPeerEvaluationResponse response(PeerEvaluationSubmission submission) {
        return response(submission, teammateAnswerRepository.findAllBySubmissionId(submission.getId()));
    }

    private MyPeerEvaluationResponse response(
        PeerEvaluationSubmission submission,
        List<PeerEvaluationTeammateAnswer> teammateAnswers
    ) {
        List<PeerEvaluationAnswerResponse> answers = teammateAnswers.stream()
            .map(answer -> PeerEvaluationAnswerResponse.teammate(
                answer.getTargetUserId(), answer.getContributionPercent(),
                answer.getContributionDetail(), answer.getTeammateAssessment()
            ))
            .collect(Collectors.toList());
        if (!isBlank(submission.getReflectionComment())) {
            answers.add(PeerEvaluationAnswerResponse.reflection(submission.getReflectionComment()));
        }
        return new MyPeerEvaluationResponse(
            submission.getId(), submission.getSelfContribution(), submission.getProjectReviewComment(),
            answers, submission.getStatus(), submission.getUpdatedAt(), submission.getSubmittedAt()
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean exceedsLimit(String value) {
        return value != null && value.trim().length() > 2000;
    }

    private static String targetRole(TeamMember member) {
        if (member.getProjectRole() != null && !member.getProjectRole().isBlank()) {
            return member.isLeader() ? "팀장 · " + member.getProjectRole() : member.getProjectRole();
        }
        return member.isLeader() ? "팀장" : "팀원";
    }

    private record Window(String state, String message) {
        static Window at(PeerEvaluationForm form, LocalDateTime now) {
            if (now.isBefore(form.getOpensAt())) {
                return new Window("UPCOMING", "상호평가가 아직 시작되지 않았습니다.");
            }
            if (!now.isBefore(form.getClosesAt())) {
                return new Window("CLOSED", "상호평가 기간이 종료되었습니다. 제출 내역만 확인할 수 있습니다.");
            }
            return new Window("OPEN", "본인을 제외한 팀원에게 기여도 합계 100%를 배분해 주세요.");
        }
    }

    private record AccessContext(PeerEvaluationForm form, List<TeamMember> targets) {
    }
}
