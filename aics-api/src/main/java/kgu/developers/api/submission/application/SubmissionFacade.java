package kgu.developers.api.submission.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import kgu.developers.api.project.presentation.response.ProjectResponse;
import kgu.developers.api.submission.presentation.request.PresentationOrderRequest;
import kgu.developers.api.submission.presentation.request.SubmissionArtifactRequest;
import kgu.developers.api.submission.presentation.request.SubmissionReopenRequest;
import kgu.developers.api.submission.presentation.response.MilestonePresentationsResponse;
import kgu.developers.api.submission.presentation.response.SubmissionArtifactResponse;
import kgu.developers.api.submission.presentation.response.SubmissionMemberConsentResponse;
import kgu.developers.api.submission.presentation.response.SubmissionResponse;
import kgu.developers.api.submission.presentation.response.SubmissionVersionDetailResponse;
import kgu.developers.api.submission.presentation.response.SubmissionVersionListResponse;
import kgu.developers.api.submission.presentation.response.TeamPresentationResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.fileobject.exception.FileObjectNotFoundException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.submission.application.command.SubmissionArtifactInput;
import kgu.developers.domain.submission.application.command.SubmissionCommandService;
import kgu.developers.domain.submission.application.query.SubmissionQueryService;
import kgu.developers.domain.submission.domain.ArtifactType;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionArtifactRepository;
import kgu.developers.domain.submission.domain.SubmissionMemberConfirmation;
import kgu.developers.domain.submission.domain.SubmissionMemberConfirmationRepository;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.domain.SubmissionVersionRepository;
import kgu.developers.domain.submission.exception.SubmissionAccessDeniedException;
import kgu.developers.domain.submission.exception.SubmissionArtifactCountMismatchException;
import kgu.developers.domain.submission.exception.SubmissionArtifactTypeRequiredException;
import kgu.developers.domain.submission.exception.SubmissionInvalidArtifactTypeException;
import kgu.developers.domain.submission.exception.SubmissionInvalidPresentationOrderException;
import kgu.developers.domain.submission.exception.SubmissionLeaderOnlyException;
import kgu.developers.domain.submission.exception.SubmissionMemberConfirmationNotApplicableException;
import kgu.developers.domain.submission.exception.SubmissionMilestoneTypeMismatchException;
import kgu.developers.domain.submission.exception.SubmissionVersionNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;

@Component
@Transactional
@RequiredArgsConstructor
public class SubmissionFacade {
    private final SubmissionCommandService submissionCommandService;
    private final SubmissionQueryService submissionQueryService;
    private final SubmissionVersionRepository submissionVersionRepository;
    private final SubmissionArtifactRepository submissionArtifactRepository;
    private final SubmissionMemberConfirmationRepository submissionMemberConfirmationRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final FileObjectRepository fileObjectRepository;
    private final FileStorage fileStorage;
    private final MilestoneRepository milestoneRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SectionQueryService sectionQueryService;
    private final UserQueryService userQueryService;

    public SubmissionResponse getMyTeamSubmission(Long milestoneId, String userId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        TeamMember member = teamMemberRepository.findActiveBySectionIdAndUserId(milestone.getSectionId(), userId)
                .orElseThrow(() -> new AccessDeniedException("그 분반의 팀 소속만 접근할 수 있습니다."));
        if (!isActiveStudent(milestone.getSectionId(), userId)) {
            throw new AccessDeniedException("그 분반에 활성 학생으로 등록된 사용자만 접근할 수 있습니다.");
        }

        Submission submission = submissionQueryService.getOrCreateSubmission(member.getTeamId(), milestoneId);
        return toResponse(submission, userId);
    }

    public SubmissionResponse getSubmission(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateActiveTeamMembership(submission, userId);
        return toResponse(submission, userId);
    }

    public SubmissionVersionListResponse getVersions(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateActiveTeamMembership(submission, userId);

        List<SubmissionVersion> versions = submissionVersionRepository.findAllBySubmissionId(submissionId);
        List<Long> versionIds = versions.stream().map(SubmissionVersion::getId).toList();

        Map<Long, List<SubmissionArtifactResponse>> artifactsByVersionId = submissionArtifactRepository
                .findAllByVersionIdIn(versionIds).stream()
                .map(this::toArtifactResponseWithVersionId)
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        Map<String, User> submittersByUserId = resolveSubmitters(versions);

        return SubmissionVersionListResponse.from(versions, artifactsByVersionId, submittersByUserId);
    }

    public SubmissionVersionDetailResponse getVersion(Long submissionId, int version, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateActiveTeamMembership(submission, userId);

        SubmissionVersion submissionVersion = submissionVersionRepository
                .findBySubmissionIdAndVersion(submissionId, version)
                .orElseThrow(SubmissionVersionNotFoundException::new);

        List<SubmissionArtifactResponse> artifacts = submissionArtifactRepository
                .findAllByVersionId(submissionVersion.getId()).stream()
                .map(this::toArtifactResponse)
                .toList();

        User submitter = resolveSubmitters(List.of(submissionVersion)).get(submissionVersion.getSubmittedBy());
        return SubmissionVersionDetailResponse.of(submissionVersion, submitter, artifacts);
    }

    private Map.Entry<Long, SubmissionArtifactResponse> toArtifactResponseWithVersionId(SubmissionArtifact artifact) {
        return Map.entry(artifact.getVersionId(), toArtifactResponse(artifact));
    }

    // 제출 이력은 그 시점의 기록이라, 제출자가 그 뒤 탈퇴(소프트 삭제)했더라도 이름이 계속
    // 보여야 한다 — 활성 사용자만 찾는 조회를 쓰면 탈퇴한 제출자의 이름이 조용히 null이 된다.
    private Map<String, User> resolveSubmitters(List<SubmissionVersion> versions) {
        List<String> submitterIds = versions.stream()
                .map(SubmissionVersion::getSubmittedBy)
                .distinct()
                .toList();
        if (submitterIds.isEmpty()) {
            return Map.of();
        }
        return userQueryService.getUsersByStudentNumbersIncludingDeleted(submitterIds).stream()
                .collect(Collectors.toMap(User::getStudentNumber, Function.identity()));
    }

    public SubmissionResponse submitVersion(
            Long submissionId,
            String userId,
            String description,
            String changeNote,
            List<SubmissionArtifactRequest> artifacts,
            List<Long> fileArtifactIds,
            List<MultipartFile> files
    ) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        Milestone milestone = validateSubmitAllowed(submission, userId);

        if (files != null && !files.isEmpty()
                && (fileArtifactIds == null || fileArtifactIds.size() != files.size())) {
            throw new SubmissionArtifactCountMismatchException();
        }

        List<SubmissionArtifactInput> inputs = new ArrayList<>();
        if (artifacts != null) {
            for (SubmissionArtifactRequest artifact : artifacts) {
                // 컨트롤러의 @Valid는 멀티파트 List 원소까지 확실히 검증하리라 보장할 수 없어서,
                // 여기서도 직접 확인한다 — 원소 자체가 null이거나(예: "artifacts": [null]) type이
                // null이면 바로 아래에서 NPE가 나 500으로 새는 대신, 의미가 분명한 400으로
                // 떨어지게 한다(sunzx0428 PR #87 리뷰 09-03 — 원소 자체가 null인 경우가 누락됨).
                if (artifact == null || artifact.type() == null) {
                    throw new SubmissionArtifactTypeRequiredException();
                }
                if (artifact.type() == ArtifactType.FILE) {
                    throw new SubmissionInvalidArtifactTypeException();
                }
                inputs.add(new SubmissionArtifactInput(
                        artifact.requiredArtifactId(), artifact.type(), null, artifact.url(), artifact.content()));
            }
        }
        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                inputs.add(new SubmissionArtifactInput(fileArtifactIds.get(i), ArtifactType.FILE, files.get(i), null, null));
            }
        }

        submissionCommandService.submitVersion(submissionId, userId, description, changeNote, inputs);

        // 최종보고서는 팀장만 제출할 수 있고(validateSubmitAllowed), 팀장의 제출 자체를
        // 팀장 본인 확인 1건으로 간주한다(프론트 요구사항) — 제출 직후 화면에 0/N이 아니라
        // 1/N로 보이게 하기 위해 여기서 바로 등록한다.
        if (milestone.getType() == MilestoneType.FINAL_REPORT) {
            submissionCommandService.confirmAsMember(submissionId, userId);
        }

        return toResponse(submissionQueryService.getSubmission(submissionId), userId);
    }

    public SubmissionMemberConsentResponse getMemberConsent(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateActiveTeamMembership(submission, userId);
        validateFinalReportMilestone(submission);
        return buildMemberConsent(submission, userId);
    }

    public SubmissionMemberConsentResponse confirmAsMember(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateActiveTeamMembership(submission, userId);
        validateFinalReportMilestone(submission);
        submissionCommandService.confirmAsMember(submissionId, userId);
        return buildMemberConsent(submissionQueryService.getSubmission(submissionId), userId);
    }

    public SubmissionMemberConsentResponse cancelConfirmation(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateActiveTeamMembership(submission, userId);
        validateFinalReportMilestone(submission);
        submissionCommandService.cancelConfirmation(submissionId, userId);
        return buildMemberConsent(submissionQueryService.getSubmission(submissionId), userId);
    }

    // 확인 조회·등록·취소는 최종보고서 전용 게이트다(Swagger·SubmissionResponse.memberConsent에
    // 이미 그렇게 문서화돼 있음) — 그 외 마일스톤에서 호출하면 확인 행이 생기지 않도록 여기서
    // 막는다(sunzx0428 PR #122 리뷰 09-06, 이전엔 마일스톤 타입 검사가 아예 없었음).
    private void validateFinalReportMilestone(Submission submission) {
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        if (milestone.getType() != MilestoneType.FINAL_REPORT) {
            throw new SubmissionMemberConfirmationNotApplicableException();
        }
    }

    // 확인 인원/전체 인원/본인 확인 여부 요약. "확인함"은 별도 필드가 아니라 이 버전에 대한
    // 확인 행이 존재하는지로 판단한다 — completeSubmission의 완료게이트(validateAllActiveMembersConfirmed)와
    // 같은 기준이다(KD3-161).
    private SubmissionMemberConsentResponse buildMemberConsent(Submission submission, String userId) {
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        Map<String, SubmissionMemberConfirmation> confirmationsByUserId = submissionMemberConfirmationRepository
                .findAllBySubmissionId(submission.getId()).stream()
                .collect(Collectors.toMap(SubmissionMemberConfirmation::getUserId, c -> c));

        List<String> activeStudentIds = teamMemberRepository.findAllByTeamId(submission.getTeamId()).stream()
                .map(TeamMember::getUserId)
                .filter(memberId -> isActiveStudent(milestone.getSectionId(), memberId))
                .toList();

        int confirmedCount = (int) activeStudentIds.stream()
                .filter(memberId -> isConfirmedForCurrentVersion(confirmationsByUserId.get(memberId), submission.getCurrentVersion()))
                .count();
        boolean isConfirmedByMe = isConfirmedForCurrentVersion(confirmationsByUserId.get(userId), submission.getCurrentVersion());

        return SubmissionMemberConsentResponse.of(confirmedCount, activeStudentIds.size(), isConfirmedByMe);
    }

    private boolean isConfirmedForCurrentVersion(SubmissionMemberConfirmation confirmation, int currentVersion) {
        return confirmation != null && confirmation.confirmsVersion(currentVersion);
    }

    public SubmissionResponse completeSubmission(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateLeader(submission, userId);
        submissionCommandService.completeSubmission(submissionId, userId);
        return toResponse(submissionQueryService.getSubmission(submissionId), userId);
    }

    public SubmissionResponse reopenSubmission(Long submissionId, String professorId, SubmissionReopenRequest request) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(milestone.getSectionId(), professorId)) {
            throw new SubmissionAccessDeniedException();
        }
        submissionCommandService.reopenSubmission(submissionId, professorId, request.revisionDueAt());
        return toResponse(submissionQueryService.getSubmission(submissionId), professorId);
    }

    public MilestonePresentationsResponse getMilestonePresentations(Long milestoneId, String userId) {
        validatePresentationMilestone(milestoneId);
        List<Submission> submissions = submissionQueryService.getSubmissionsOrderedForPresentation(milestoneId);
        List<Long> teamIds = submissions.stream().map(Submission::getTeamId).distinct().toList();

        Map<Long, String> teamNames = teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getId, Team::getName));

        Map<Long, Project> projectByTeamId = projectRepository.findAllByTeamIdIn(teamIds).stream()
                .collect(Collectors.toMap(Project::getTeamId, p -> p, (a, b) -> a));

        Map<Long, Set<String>> memberIdsByTeamId = teamMemberRepository.findAllByTeamIdIn(teamIds).stream()
                .collect(Collectors.groupingBy(
                        TeamMember::getTeamId,
                        Collectors.mapping(TeamMember::getUserId, Collectors.toSet())));

        Map<Long, List<SubmissionArtifactResponse>> artifactsBySubmissionId =
                resolveLatestSubmissionArtifacts(submissions);

        List<TeamPresentationResponse> contents = submissions.stream()
                .map(submission -> {
                    Long teamId = submission.getTeamId();
                    String teamName = teamNames.get(teamId);
                    Project project = projectByTeamId.get(teamId);
                    Set<String> memberIds = memberIdsByTeamId.getOrDefault(teamId, Set.of());
                    ProjectResponse projectResponse = (project != null)
                            ? ProjectResponse.from(project, resolveProjectScreenImageUrls(memberIds, project.getScreenConfiguration()))
                            : null;
                    List<SubmissionArtifactResponse> artifacts =
                            artifactsBySubmissionId.getOrDefault(submission.getId(), List.of());
                    return TeamPresentationResponse.of(submission, teamName, projectResponse, artifacts);
                })
                .toList();

        return MilestonePresentationsResponse.builder().contents(contents).build();
    }

    private Map<Long, List<SubmissionArtifactResponse>> resolveLatestSubmissionArtifacts(List<Submission> submissions) {
        Map<Long, Integer> currentVersionBySubmissionId = submissions.stream()
                .filter(s -> s.getCurrentVersion() > 0)
                .collect(Collectors.toMap(Submission::getId, Submission::getCurrentVersion));

        if (currentVersionBySubmissionId.isEmpty()) {
            return Map.of();
        }

        List<Long> submissionIds = currentVersionBySubmissionId.keySet().stream().toList();
        List<SubmissionVersion> allVersions = submissionVersionRepository.findAllBySubmissionIdIn(submissionIds);

        Map<Long, SubmissionVersion> latestVersionBySubmissionId = allVersions.stream()
                .filter(v -> Objects.equals(v.getVersion(), currentVersionBySubmissionId.get(v.getSubmissionId())))
                .collect(Collectors.toMap(SubmissionVersion::getSubmissionId, v -> v, (a, b) -> a));

        List<Long> versionIds = latestVersionBySubmissionId.values().stream()
                .map(SubmissionVersion::getId)
                .toList();

        Map<Long, List<SubmissionArtifact>> artifactsByVersionId = submissionArtifactRepository
                .findAllByVersionIdIn(versionIds).stream()
                .collect(Collectors.groupingBy(SubmissionArtifact::getVersionId));

        Map<Long, List<SubmissionArtifactResponse>> result = new HashMap<>();
        for (Map.Entry<Long, SubmissionVersion> entry : latestVersionBySubmissionId.entrySet()) {
            Long submissionId = entry.getKey();
            SubmissionVersion version = entry.getValue();
            List<SubmissionArtifact> artifacts = artifactsByVersionId.getOrDefault(version.getId(), List.of());
            result.put(submissionId, artifacts.stream().map(this::toArtifactResponse).toList());
        }
        return result;
    }

    // screenConfiguration은 [{title, description, imageFileId}, ...]를 원본 그대로 저장·응답하는데,
    // imageFileId만 내려주면 다른 팀 사용자는 그 이미지를 실제로 볼 방법이 없다(presigned URL을 받는 경로가
    // 이 발표자료 조회 API 말고는 없음). 조회할 때마다 각 화면의 imageFileId를 presigned URL(imageUrl)로
    // 보강해서 내려준다 — 저장은 안 건드린다(15분 후 만료되는 임시 URL이라 영구 저장하면 안 됨).
    private JsonNode resolveProjectScreenImageUrls(Set<String> memberIds, JsonNode screens) {
        if (screens == null || !screens.isArray()) {
            return screens;
        }
        ArrayNode resolved = JsonNodeFactory.instance.arrayNode();
        for (JsonNode screen : screens) {
            if (!screen.isObject()) {
                resolved.add(screen);
                continue;
            }
            ObjectNode sanitized = (ObjectNode) screen.deepCopy();
            sanitized.remove("imageUrl");
            JsonNode imageFileIdNode = screen.get("imageFileId");
            if (imageFileIdNode != null && imageFileIdNode.isIntegralNumber()) {
                fileObjectRepository.findById(imageFileIdNode.asLong())
                        .filter(fileObject -> memberIds.contains(fileObject.getUploadedBy()))
                        .ifPresent(fileObject -> sanitized.put("imageUrl", fileStorage.presignedUrl(fileObject.getStorageKey())));
            }
            resolved.add(sanitized);
        }
        return resolved;
    }

    public void assignPresentationOrder(Long milestoneId, String professorId, PresentationOrderRequest request) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        if (milestone.getType() != MilestoneType.PRESENTATION) {
            throw new SubmissionMilestoneTypeMismatchException();
        }
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(milestone.getSectionId(), professorId)) {
            throw new SubmissionAccessDeniedException();
        }
        // Collectors.toMap은 키가 중복되면 IllegalStateException(500)을 던지므로, 여기서
        // 직접 넣으면서 중복 teamId를 400으로 미리 걸러낸다.
        Map<Long, Integer> orderByTeamId = new LinkedHashMap<>();
        for (PresentationOrderRequest.TeamOrder teamOrder : request.teamOrders()) {
            if (orderByTeamId.put(teamOrder.teamId(), teamOrder.order()) != null) {
                throw new SubmissionInvalidPresentationOrderException();
            }
        }
        submissionCommandService.assignPresentationOrders(milestoneId, orderByTeamId);
    }

    // 최종보고서 파일 제출은 팀장만 가능하다(프론트 요구사항). 그 외 마일스톤은
    // 기존대로 활성 팀원이면 누구나 제출할 수 있다.
    private Milestone validateSubmitAllowed(Submission submission, String userId) {
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        if (milestone.getType() == MilestoneType.FINAL_REPORT) {
            validateLeader(submission, userId);
        } else {
            validateActiveTeamMembership(submission, userId);
        }
        return milestone;
    }

    // 탈퇴했거나 조교로 전환된 기존 팀장이 계속 완료 처리할 수 있던 구멍을 막기 위해,
    // 팀장 여부뿐 아니라 지금도 그 분반의 활성 학생인지까지 같이 확인한다.
    private void validateLeader(Submission submission, String userId) {
        validateActiveTeamMembership(submission, userId);
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(submission.getTeamId(), userId)
                .orElseThrow(SubmissionAccessDeniedException::new);
        if (!member.isLeader()) {
            throw new SubmissionLeaderOnlyException();
        }
    }

    private SubmissionResponse toResponse(Submission submission, String userId) {
        return SubmissionResponse.of(
                submission,
                submissionQueryService.canSubmitNow(submission),
                submissionQueryService.hasPendingReview(submission),
                buildMemberConsentForResponse(submission, userId)
        );
    }

    // toResponse에 임베드할 때만 최종보고서 마일스톤으로 한정한다 — 그 외 마일스톤은 이 게이트
    // 자체가 없으므로(PRD, 최종보고서 전용) null로 둔다. 전용 확인/취소 API(getMemberConsent 등)는
    // 마일스톤 타입과 무관하게 항상 계산하므로 그쪽 buildMemberConsent()는 그대로 두고,
    // 여기서만 감싸서 게이트를 추가한다.
    private SubmissionMemberConsentResponse buildMemberConsentForResponse(Submission submission, String userId) {
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        if (milestone.getType() != MilestoneType.FINAL_REPORT) {
            return null;
        }
        return buildMemberConsent(submission, userId);
    }

    private SubmissionArtifactResponse toArtifactResponse(SubmissionArtifact artifact) {
        if (artifact.getType() != ArtifactType.FILE) {
            return SubmissionArtifactResponse.of(artifact);
        }
        FileObject fileObject = fileObjectRepository.findById(artifact.getFileId())
                .orElseThrow(FileObjectNotFoundException::new);
        String downloadUrl = fileStorage.presignedUrl(fileObject.getStorageKey());
        return SubmissionArtifactResponse.ofFile(artifact, fileObject, downloadUrl);
    }

    private void validateTeamMembership(Long teamId, String userId) {
        if (teamMemberRepository.findByTeamIdAndUserId(teamId, userId).isEmpty()) {
            throw new AccessDeniedException("그 팀에 소속된 사용자만 접근할 수 있습니다.");
        }
    }

    // 쓰기 경로(제출/확인/발표자료 수정) 전용 — 팀원 행이 남아있는 것만으로는 부족하고,
    // 지금 이 분반에 "활성 학생"으로 등록돼 있어야 한다. 탈퇴했거나 조교로 역할이 바뀐 뒤에도
    // TeamMember 행만 안 지워지면 계속 쓸 수 있던 구멍을 막는다.
    private void validateActiveTeamMembership(Submission submission, String userId) {
        validateTeamMembership(submission.getTeamId(), userId);
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        if (!isActiveStudent(milestone.getSectionId(), userId)) {
            throw new AccessDeniedException("그 분반에 활성 학생으로 등록된 사용자만 접근할 수 있습니다.");
        }
    }

    private boolean isActiveStudent(Long sectionId, String userId) {
        return enrollmentRepository.findBySectionIdAndUserId(sectionId, userId)
                .map(Enrollment::isActiveStudent)
                .orElse(false);
    }

    private void validatePresentationMilestone(Long milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        if (milestone.getType() != MilestoneType.PRESENTATION) {
            throw new SubmissionMilestoneTypeMismatchException();
        }
    }
}
