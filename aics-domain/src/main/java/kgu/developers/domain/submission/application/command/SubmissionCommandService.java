package kgu.developers.domain.submission.application.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Status;
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
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.domain.SubmissionVersionRepository;
import kgu.developers.domain.submission.exception.SubmissionMemberConfirmationIncompleteException;
import kgu.developers.domain.submission.exception.SubmissionNotAllowedNowException;
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
    private final SubmissionQueryService submissionQueryService;

    public SubmissionVersion submitVersion(
            Long submissionId,
            String userId,
            String description,
            String changeNote,
            List<SubmissionArtifactInput> artifactInputs
    ) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        if (!submissionQueryService.canSubmitNow(submission)) {
            throw new SubmissionNotAllowedNowException();
        }

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
    public SubmissionMemberConfirmation confirmAsMember(
            Long submissionId,
            String userId,
            boolean confirmedFinalReport,
            boolean confirmedArtifacts,
            String oneLineReview
    ) {
        SubmissionMemberConfirmation confirmation = submissionMemberConfirmationRepository
                .findBySubmissionIdAndUserId(submissionId, userId)
                .orElse(null);
        SubmissionMemberConfirmation toSave = SubmissionMemberConfirmation.builder()
                .id(confirmation != null ? confirmation.getId() : null)
                .submissionId(submissionId)
                .userId(userId)
                .confirmedFinalReport(confirmedFinalReport)
                .confirmedArtifacts(confirmedArtifacts)
                .oneLineReview(oneLineReview)
                .confirmedAt(LocalDateTime.now())
                .build();
        return submissionMemberConfirmationRepository.save(toSave);
    }

    // 최종보고서 마일스톤이면 팀원 전원(WITHDRAWN 제외) 확인이 끝나야 통과시킨다.
    // 그 외 마일스톤은 게이트 자체가 없어 호출만 되면 바로 끝난다(문서화된 동작).
    public void completeSubmission(Long submissionId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));

        if (milestone.getType() != MilestoneType.FINAL_REPORT) {
            return;
        }

        List<TeamMember> members = teamMemberRepository.findAllByTeamId(submission.getTeamId());
        Set<String> confirmedUserIds = submissionMemberConfirmationRepository
                .findAllBySubmissionId(submissionId).stream()
                .map(SubmissionMemberConfirmation::getUserId)
                .collect(Collectors.toSet());

        boolean allActiveMembersConfirmed = members.stream()
                .filter(member -> isActiveEnrollment(milestone.getSectionId(), member.getUserId()))
                .allMatch(member -> confirmedUserIds.contains(member.getUserId()));

        if (!allActiveMembersConfirmed) {
            throw new SubmissionMemberConfirmationIncompleteException();
        }
    }

    private boolean isActiveEnrollment(Long sectionId, String userId) {
        return enrollmentRepository.findBySectionIdAndUserId(sectionId, userId)
                .map(enrollment -> enrollment.getStatus() == Status.ACTIVE)
                .orElse(false);
    }

    // "재오픈이 정확히 무엇을 되돌리는지"는 팀 미결정사항(#3) — 지금은 이 팀·마일스톤 한정으로
    // revisionDueAt까지 재제출을 허용하는 최소 구현. 팀 결정이 나오면 다시 손봐야 한다.
    public void reopenSubmission(Long submissionId, String professorId, LocalDateTime revisionDueAt) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        submission.reopen(professorId, revisionDueAt);
        submissionRepository.save(submission);
    }

    // 교수가 드래그앤드롭으로 지정한 발표 순서를 일괄 반영한다. 순서 로직 자체는 없음(임의 지정, PRD 그대로).
    public void assignPresentationOrders(Long milestoneId, Map<Long, Integer> orderByTeamId) {
        List<Submission> submissions = submissionRepository.findAllByMilestoneId(milestoneId);
        for (Submission submission : submissions) {
            Integer order = orderByTeamId.get(submission.getTeamId());
            if (order != null) {
                submission.assignPresentationOrder(order);
                submissionRepository.save(submission);
            }
        }
    }

    private boolean isLate(Submission submission) {
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        LocalDateTime dueAt = milestone.getSchedule().dueAt();
        return dueAt != null && LocalDateTime.now().isAfter(dueAt);
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
        return SubmissionArtifact.text(versionId, input.requiredArtifactId(), input.content());
    }
}
