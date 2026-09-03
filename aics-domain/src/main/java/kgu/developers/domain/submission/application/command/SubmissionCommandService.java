package kgu.developers.domain.submission.application.command;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactRepository;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.submission.application.query.SubmissionQueryService;
import kgu.developers.domain.submission.domain.ArtifactType;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionArtifactRepository;
import kgu.developers.domain.submission.domain.SubmissionMemberConfirmation;
import kgu.developers.domain.submission.domain.SubmissionMemberConfirmationRepository;
import kgu.developers.domain.submission.domain.SubmissionRepository;
import kgu.developers.domain.submission.exception.SubmissionNotFoundException;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.domain.SubmissionVersionRepository;
import kgu.developers.domain.submission.exception.SubmissionMemberConfirmationIncompleteException;
import kgu.developers.domain.submission.exception.SubmissionNotAllowedNowException;
import kgu.developers.domain.submission.exception.SubmissionNotCompletedException;
import kgu.developers.domain.submission.exception.SubmissionNotYetSubmittedException;
import kgu.developers.domain.submission.exception.SubmissionInvalidPresentationOrderException;
import kgu.developers.domain.submission.exception.SubmissionRequiredArtifactMismatchException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmissionCommandService {
    private final SubmissionRepository submissionRepository;
    private final SubmissionVersionRepository submissionVersionRepository;
    private final SubmissionArtifactRepository submissionArtifactRepository;
    private final SubmissionMemberConfirmationRepository submissionMemberConfirmationRepository;
    private final FileObjectRepository fileObjectRepository;
    private final FileStorage fileStorage;
    private final MilestoneRepository milestoneRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RequiredArtifactRepository requiredArtifactRepository;
    private final TeamRepository teamRepository;
    private final SubmissionQueryService submissionQueryService;

    public SubmissionVersion submitVersion(
            Long submissionId,
            String userId,
            String description,
            String changeNote,
            List<SubmissionArtifactInput> artifactInputs
    ) {
        // 상태 전환(제출·완료·재개)과 버전 채번을 같은 잠금 규약으로 직렬화한다 — 잠금 없이
        // countBySubmissionId만으로 다음 버전을 계산하면 동시 제출 시 같은 번호가 나올 수 있고,
        // 완료·재개와 겹치면 나중에 저장한 쪽이 먼저 것을 조용히 덮어쓸 수 있다
        // (sunzx0428 PR #87 리뷰 09-03).
        Submission submission = submissionRepository.findByIdForUpdate(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);
        if (!submissionQueryService.canSubmitNow(submission)) {
            throw new SubmissionNotAllowedNowException();
        }
        validateAgainstRequiredArtifacts(submission.getMilestoneId(), artifactInputs);

        int nextVersion = submissionVersionRepository.countBySubmissionId(submissionId) + 1;
        SubmissionVersion version = submissionVersionRepository.save(SubmissionVersion.create(
                submissionId, nextVersion, description, changeNote, userId, isLate(submission)));

        List<SubmissionArtifact> artifacts = artifactInputs.stream()
                .map(input -> toArtifact(version.getId(), userId, input))
                .toList();
        if (!artifacts.isEmpty()) {
            submissionArtifactRepository.saveAll(artifacts);
        }

        submission.recordNewVersion(nextVersion);
        submissionRepository.save(submission);

        return version;
    }

    // 팀원 본인의 확인을 등록/갱신한다(최종보고서 게이트용). 이미 확인한 적 있으면 덮어쓴다.
    // 확인은 "지금 이 순간의 currentVersion"에 묶인다 — 재제출로 버전이 올라가면 예전 확인은
    // 더 이상 이 게이트를 통과시키지 못한다(SubmissionMemberConfirmation.confirmsVersion 참고).
    public SubmissionMemberConfirmation confirmAsMember(
            Long submissionId,
            String userId,
            boolean confirmedFinalReport,
            boolean confirmedArtifacts,
            String oneLineReview
    ) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        SubmissionMemberConfirmation confirmation = submissionMemberConfirmationRepository
                .findBySubmissionIdAndUserId(submissionId, userId)
                .orElse(null);
        SubmissionMemberConfirmation toSave = SubmissionMemberConfirmation.builder()
                .id(confirmation != null ? confirmation.getId() : null)
                .submissionId(submissionId)
                .userId(userId)
                .version(submission.getCurrentVersion())
                .confirmedFinalReport(confirmedFinalReport)
                .confirmedArtifacts(confirmedArtifacts)
                .oneLineReview(oneLineReview)
                .confirmedAt(LocalDateTime.now())
                .build();
        return submissionMemberConfirmationRepository.save(toSave);
    }

    // 최종보고서 마일스톤이면 팀원 전원(WITHDRAWN 제외)이 "지금 버전"을 실제로(true/true) 확인해야 통과한다.
    // 그 외 마일스톤은 게이트 자체가 없다(문서화된 동작). 미제출 상태는 애초에 완료 대상이 아니다.
    public void completeSubmission(Long submissionId, String completedBy) {
        // submitVersion과 같은 잠금 규약: 제출·재개와 경합해도 상태가 유실되지 않게 잠근다.
        Submission submission = submissionRepository.findByIdForUpdate(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);
        if (!submission.isSubmitted()) {
            throw new SubmissionNotYetSubmittedException();
        }
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));

        if (milestone.getType() == MilestoneType.FINAL_REPORT) {
            validateAllActiveMembersConfirmed(submission, milestone);
        }

        submission.complete(completedBy);
        submissionRepository.save(submission);
    }

    private void validateAllActiveMembersConfirmed(Submission submission, Milestone milestone) {
        List<TeamMember> members = teamMemberRepository.findAllByTeamId(submission.getTeamId());
        Map<String, SubmissionMemberConfirmation> confirmationsByUserId = submissionMemberConfirmationRepository
                .findAllBySubmissionId(submission.getId()).stream()
                .collect(Collectors.toMap(SubmissionMemberConfirmation::getUserId, c -> c));

        boolean allActiveMembersConfirmed = members.stream()
                .filter(member -> isActiveStudent(milestone.getSectionId(), member.getUserId()))
                .allMatch(member -> {
                    SubmissionMemberConfirmation confirmation = confirmationsByUserId.get(member.getUserId());
                    return confirmation != null
                            && confirmation.confirmsVersion(submission.getCurrentVersion())
                            && confirmation.isFullyConfirmed();
                });

        if (!allActiveMembersConfirmed) {
            throw new SubmissionMemberConfirmationIncompleteException();
        }
    }

    // 활성 조교(ASSISTANT)는 확인 대상이 아니다 — 최종보고서 확인은 활성 STUDENT만 대상으로 한다(팀 합의).
    private boolean isActiveStudent(Long sectionId, String userId) {
        return enrollmentRepository.findBySectionIdAndUserId(sectionId, userId)
                .map(Enrollment::isActiveStudent)
                .orElse(false);
    }

    // "재오픈이 정확히 무엇을 되돌리는지"는 팀 미결정사항(#3) — 지금은 이 팀·마일스톤 한정으로
    // revisionDueAt까지 재제출을 허용하는 최소 구현. 팀 결정이 나오면 다시 손봐야 한다.
    // 완료된 제출만 재오픈 대상이다(미완료 상태는 이미 제출 기간 로직으로 커버됨).
    public void reopenSubmission(Long submissionId, String professorId, LocalDateTime revisionDueAt) {
        // submitVersion과 같은 잠금 규약: 제출·완료와 경합해도 상태가 유실되지 않게 잠근다.
        Submission submission = submissionRepository.findByIdForUpdate(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);
        if (!submission.isCompleted()) {
            throw new SubmissionNotCompletedException();
        }
        submission.reopen(professorId, revisionDueAt);
        submissionRepository.save(submission);
    }

    // 교수가 드래그앤드롭으로 지정한 발표 순서를 일괄 반영한다. 순서 로직 자체는 없음(임의 지정, PRD 그대로).
    // Submission은 "우리팀 제출 조회" 시점에 lazy 생성되는데, 발표순서 지정은 그 조회 여부와
    // 무관하게 분반의 팀 전체를 대상으로 해야 하므로, 아직 조회 안 해본 팀은 여기서 만들어준다.
    public void assignPresentationOrders(Long milestoneId, Map<Long, Integer> orderByTeamId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        List<Team> sectionTeams = teamRepository.findAllBySectionId(milestone.getSectionId());
        validatePresentationOrders(sectionTeams, orderByTeamId);

        for (Team team : sectionTeams) {
            Submission submission = submissionQueryService.getOrCreateSubmission(team.getId(), milestoneId);
            submission.assignPresentationOrder(orderByTeamId.get(team.getId()));
            submissionRepository.save(submission);
        }
    }

    private void validatePresentationOrders(List<Team> sectionTeams, Map<Long, Integer> orderByTeamId) {
        Set<Long> sectionTeamIds = sectionTeams.stream().map(Team::getId).collect(Collectors.toSet());
        if (!orderByTeamId.keySet().equals(sectionTeamIds)) {
            throw new SubmissionInvalidPresentationOrderException();
        }
        boolean hasNonPositiveOrder = orderByTeamId.values().stream().anyMatch(order -> order == null || order <= 0);
        if (hasNonPositiveOrder) {
            throw new SubmissionInvalidPresentationOrderException();
        }
        long distinctOrderCount = orderByTeamId.values().stream().distinct().count();
        if (distinctOrderCount != orderByTeamId.size()) {
            throw new SubmissionInvalidPresentationOrderException();
        }
    }

    private boolean isLate(Submission submission) {
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        LocalDateTime dueAt = milestone.getSchedule().dueAt();
        return dueAt != null && LocalDateTime.now().isAfter(dueAt);
    }

    // 이 마일스톤의 RequiredArtifact 구성 기준으로 소유관계/타입/필수여부/확장자/용량을 검증한다.
    // requiredArtifactId가 없는 입력(자유 형식 산출물)은 이 검증 대상이 아니다.
    private void validateAgainstRequiredArtifacts(Long milestoneId, List<SubmissionArtifactInput> artifactInputs) {
        List<RequiredArtifact> requiredArtifacts = requiredArtifactRepository.findAllByMilestoneId(milestoneId);
        Map<Long, RequiredArtifact> requiredById = requiredArtifacts.stream()
                .collect(Collectors.toMap(RequiredArtifact::getId, Function.identity()));

        Set<Long> submittedRequiredIds = new HashSet<>();
        for (SubmissionArtifactInput input : artifactInputs) {
            if (input.requiredArtifactId() == null) {
                continue;
            }
            RequiredArtifact required = requiredById.get(input.requiredArtifactId());
            if (required == null || !required.getType().name().equals(input.type().name())) {
                throw new SubmissionRequiredArtifactMismatchException();
            }
            validatePayload(required, input);
            submittedRequiredIds.add(required.getId());
        }

        boolean missingRequired = requiredArtifacts.stream()
                .filter(RequiredArtifact::isRequired)
                .anyMatch(required -> !submittedRequiredIds.contains(required.getId()));
        if (missingRequired) {
            throw new SubmissionRequiredArtifactMismatchException();
        }
    }

    // FILE은 파일 자체(용량/확장자)를, LINK/CHEERPJ_RUN은 url을, TEXT는 본문을 검증한다.
    // 타입만 맞고 실제 내용이 비어있는 입력이 필수 산출물 체크를 조용히 통과하는 것을 막는다.
    private void validatePayload(RequiredArtifact required, SubmissionArtifactInput input) {
        switch (input.type()) {
            case FILE -> validateFile(required, input.file());
            case LINK, CHEERPJ_RUN -> validateNotBlank(input.url());
            case TEXT -> validateNotBlank(input.content());
        }
    }

    private void validateNotBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new SubmissionRequiredArtifactMismatchException();
        }
    }

    private void validateFile(RequiredArtifact required, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SubmissionRequiredArtifactMismatchException();
        }
        if (required.getAllowedExtensions() != null) {
            String filename = file.getOriginalFilename();
            String extension = (filename != null && filename.contains("."))
                    ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                    : "";
            List<String> allowedExtensions = Arrays.stream(required.getAllowedExtensions().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toList();
            if (!allowedExtensions.contains(extension)) {
                throw new SubmissionRequiredArtifactMismatchException();
            }
        }
        if (required.getMaxFileSizeMb() != null && file.getSize() > required.getMaxFileSizeMb() * 1024L * 1024L) {
            throw new SubmissionRequiredArtifactMismatchException();
        }
    }

    private SubmissionArtifact toArtifact(Long versionId, String userId, SubmissionArtifactInput input) {
        if (input.type() == ArtifactType.FILE) {
            String storageKey = fileStorage.upload(input.file());
            FileObject fileObject = fileObjectRepository.save(FileObject.create(
                    userId,
                    storageKey,
                    input.file().getOriginalFilename(),
                    input.file().getContentType(),
                    input.file().getSize(),
                    false,
                    null
            ));
            return SubmissionArtifact.file(versionId, input.requiredArtifactId(), fileObject.getId());
        }
        if (input.type() == ArtifactType.LINK) {
            return SubmissionArtifact.link(versionId, input.requiredArtifactId(), input.url());
        }
        if (input.type() == ArtifactType.CHEERPJ_RUN) {
            return SubmissionArtifact.cheerpjRun(versionId, input.requiredArtifactId(), input.url());
        }
        return SubmissionArtifact.text(versionId, input.requiredArtifactId(), input.content());
    }
}
