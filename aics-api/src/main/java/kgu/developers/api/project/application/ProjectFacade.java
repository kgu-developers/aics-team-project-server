package kgu.developers.api.project.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.project.presentation.request.ProposalSectionRequest;
import jakarta.validation.Validator;
import kgu.developers.api.project.presentation.response.ProjectImageUploadResponse;
import kgu.developers.api.project.presentation.response.ProjectResponse;
import kgu.developers.api.project.presentation.response.ProjectApprovalSummaryResponse;
import kgu.developers.api.project.presentation.response.ProposalSectionListResponse;
import kgu.developers.api.project.presentation.response.ProposalSectionResponse;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.team.application.TeamFacade;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.fileobject.exception.FileObjectInvalidTypeException;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.application.query.ProjectQueryService;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.exception.ProjectVersionConflictException;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.editlock.exception.EditLockConflictException;
import kgu.developers.domain.project.domain.ProposalSection;
import kgu.developers.domain.project.domain.ProposalSectionRepository;
import kgu.developers.domain.project.domain.ProposalSectionType;
import kgu.developers.domain.project.exception.ProjectScreenImageOwnershipException;
import kgu.developers.domain.projectApproval.application.command.ProjectApprovalCommandService;
import kgu.developers.domain.projectApproval.domain.ApprovalCount;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Component
@RequiredArgsConstructor
@Transactional
public class ProjectFacade {

    private static final int MAX_IMAGE_DIMENSION = 4_096;
    private static final long MAX_IMAGE_PIXELS = 16_000_000L;
    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final ProjectCommandService projectCommandService;
    private final ProjectRepository projectRepository;
    private final EditLockQueryService editLockQueryService;
    private final ProjectQueryService projectQueryService;
    private final TeamAccessValidator teamAccessValidator;
    private final ProjectApprovalRepository projectApprovalRepository;
    private final ProjectApprovalCommandService projectApprovalCommandService;
    private final TeamMemberRepository teamMemberRepository;
    private final FileObjectRepository fileObjectRepository;
    private final FileStorage fileStorage;
    private final ProposalSectionRepository proposalSectionRepository;
    private final UserRepository userRepository;
    private final TeamFacade teamFacade;
    private final Validator validator;
    private final TeamRepository teamRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PlatformTransactionManager transactionManager;

    // 호출자의 트랜잭션이 팀 잠금과 URL 생성까지 묶지 않도록 스냅샷 트랜잭션을 분리한다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProjectResponse getProject(Long teamId, String userId) {
        ProjectSnapshot snapshot = new TransactionTemplate(transactionManager).execute(status -> {
            teamAccessValidator.validateMembershipOrProfessorBeforeLock(teamId, userId);
            // 접근 검증도 Team을 읽으므로, 킥오프 본문과 섹션 버전을 읽기 전에 잠근다.
            projectRepository.lockTeam(teamId);
            teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
            Project project = projectQueryService.getProjectByTeamId(teamId);
            return projectSnapshot(project, userId);
        });
        return resolveSnapshot(snapshot);
    }

    public ProjectResponse saveProject(Long teamId, String userId, ProjectRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new jakarta.validation.ConstraintViolationException(violations);
        }
        ProposalSectionType type = request.sectionKey();
        teamAccessValidator.validateMembershipBeforeLock(teamId, userId);
        // 팀원 역할도 응답 본문에 포함되므로 접근 검증이 읽기 전에 잠근다.
        projectRepository.lockTeam(teamId);
        teamAccessValidator.validateMembership(teamId, userId);
        boolean activeStudent = teamRepository.findSectionIdById(teamId)
            .flatMap(sectionId -> enrollmentRepository.findBySectionIdAndUserId(sectionId, userId))
            .map(Enrollment::isActiveStudent)
            .orElse(false);
        if (!activeStudent) {
            throw new AccessDeniedException("그 분반에 활성 학생으로 등록된 사용자만 편집할 수 있습니다.");
        }
        Project current = projectQueryService.getProjectByTeamId(teamId);
        if (current.getProposalCompletedAt() != null) {
            throw new kgu.developers.domain.project.exception.ProjectProposalCompletedException();
        }
        ProposalSection section = projectCommandService.getOrCreateSection(current, type);
        if (request.sectionVersion() != section.getContentVersion()) {
            throw new ProjectVersionConflictException();
        }
        editLockQueryService.getActiveLock(EditLockTargetType.PROJECT, current.getId(), type.name())
            .filter(lock -> lock.isOwnedBy(userId))
            .orElseThrow(EditLockConflictException::new);

        if (type == ProposalSectionType.SCREEN) {
            validateScreenImagesOwnedByTeam(teamId, request.screenConfiguration());
        }
        if (type == ProposalSectionType.TEAM_OPERATION
            && (request.kickoffRule() != null || request.meetingSchedule() != null || request.memberRoles() != null)) {
            teamFacade.updateKickoffContent(teamId, userId,
                request.kickoffRule(), request.meetingSchedule(), request.memberRoles());
        }
        Project saved = projectCommandService.saveProject(
            teamId,
            type == ProposalSectionType.TOPIC ? request.title() : current.getTitle(),
            type == ProposalSectionType.TOPIC ? request.description() : current.getDescription(),
            type == ProposalSectionType.TOPIC ? request.goal() : current.getGoal(),
            type == ProposalSectionType.TOPIC ? request.repositoryUrl() : current.getRepositoryUrl(),
            type == ProposalSectionType.TOPIC ? request.externalLinks() : current.getExternalLinks(),
            type == ProposalSectionType.DATA ? request.dataConfiguration() : current.getDataConfiguration(),
            type == ProposalSectionType.SCREEN ? stripClientProvidedImageUrls(request.screenConfiguration()) : current.getScreenConfiguration(),
            type == ProposalSectionType.TEAM_OPERATION && request.projectSchedule() != null
                ? request.projectSchedule() : current.getProjectSchedule()
        );
        return resolveSnapshot(projectSnapshot(saved, userId));
    }

    private ProjectSnapshot projectSnapshot(Project project, String userId) {
        Map<ProposalSectionType, Long> versions = projectCommandService.getOrCreateSectionVersions(project);
        Map<Long, String> imageStorageKeys = new HashMap<>();
        JsonNode screens = prepareScreenImages(project.getTeamId(), project.getScreenConfiguration(), imageStorageKeys);
        ProjectResponse response = ProjectResponse.from(project, screens,
            teamFacade.getKickoffByTeamId(project.getTeamId(), userId), Map.copyOf(versions)).toBuilder()
            .dataConfiguration(project.getDataConfiguration() == null ? null : project.getDataConfiguration().deepCopy())
            .externalLinks(project.getExternalLinks() == null ? null : project.getExternalLinks().deepCopy())
            .build();
        return new ProjectSnapshot(response, Map.copyOf(imageStorageKeys));
    }

    private record ProjectSnapshot(ProjectResponse response, Map<Long, String> imageStorageKeys) {}

    private ProjectResponse resolveSnapshot(ProjectSnapshot snapshot) {
        JsonNode screens = snapshot.response().screenConfiguration();
        if (screens != null && screens.isArray()) {
            for (JsonNode screen : screens) {
                JsonNode imageFileId = screen.get("imageFileId");
                if (screen.isObject() && imageFileId != null && imageFileId.isIntegralNumber()) {
                    String storageKey = snapshot.imageStorageKeys().get(imageFileId.asLong());
                    if (storageKey != null) {
                        ((ObjectNode) screen).put("imageUrl", fileStorage.presignedUrl(storageKey));
                    }
                }
            }
        }
        return snapshot.response();
    }

    public ProjectImageUploadResponse uploadProjectImage(Long teamId, String userId, MultipartFile file) {
        teamAccessValidator.validateMembership(teamId, userId);
        String contentType = file == null || file.isEmpty() ? null : imageContentType(file);
        if (contentType == null) {
            throw new FileObjectInvalidTypeException();
        }

        String fileName = imageFileName(file.getOriginalFilename());
        String storageKey = fileStorage.upload(file, contentType, fileName);
        FileObject saved;
        try {
            saved = fileObjectRepository.save(FileObject.create(
                userId, storageKey, fileName, contentType, file.getSize(), false, null
            ));
        } catch (RuntimeException exception) {
            try {
                fileStorage.delete(storageKey);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
        return new ProjectImageUploadResponse(saved.getId());
    }

    private String imageFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "image";
        }
        return originalFilename.length() <= MAX_FILE_NAME_LENGTH
            ? originalFilename
            : originalFilename.substring(0, MAX_FILE_NAME_LENGTH);
    }

    private String imageContentType(MultipartFile file) {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                String contentType = switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpg", "jpeg" -> "image/jpeg";
                    case "png", "gif", "bmp" -> "image/" + reader.getFormatName().toLowerCase(Locale.ROOT);
                    default -> null;
                };
                if (contentType == null) {
                    return null;
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
                    || (long) width * height > MAX_IMAGE_PIXELS) {
                    return null;
                }
                return contentType;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    public void completeProposal(Long projectId, String userId) {
        Project project = projectQueryService.getProject(projectId);
        
        projectCommandService.lockTeam(project.getTeamId());
        teamAccessValidator.validateLeader(project.getTeamId(), userId);

        projectCommandService.completeProposal(projectId);
    }

    public void deleteProject(Long projectId, String userId) {
        Project project = projectQueryService.getProject(projectId);
        teamAccessValidator.validateLeader(project.getTeamId(), userId);

        projectCommandService.deleteProject(projectId);
    }

    public void approveProject(Long projectId, String userId) {
        Project project = projectQueryService.getProject(projectId);
        teamAccessValidator.validateMembership(project.getTeamId(), userId);
        projectApprovalCommandService.approve(projectId, userId, LocalDateTime.now());
    }

    public ProposalSectionListResponse getProposalSections(Long projectId, String userId) {
        Project project = projectQueryService.getProject(projectId);
        teamAccessValidator.validateMembershipOrProfessor(project.getTeamId(), userId);

        Map<ProposalSectionType, ProposalSection> saved = proposalSectionRepository.findAllByProjectId(projectId).stream()
            .collect(toMap(ProposalSection::getType, Function.identity()));
        Map<String, String> names = assigneeNames(saved.values());

        List<ProposalSectionResponse> contents = Arrays.stream(ProposalSectionType.values())
            .map(type -> saved.containsKey(type)
                ? ProposalSectionResponse.of(saved.get(type), names.get(saved.get(type).getAssigneeUserId()))
                : ProposalSectionResponse.empty(type))
            .toList();
        return ProposalSectionListResponse.from(contents);
    }

    public ProposalSectionResponse updateProposalSection(
        Long projectId,
        ProposalSectionType type,
        String userId,
        ProposalSectionRequest request
    ) {
        Project project = projectQueryService.getProject(projectId);
        teamAccessValidator.validateMembership(project.getTeamId(), userId);

        ProposalSection section = projectCommandService.updateProposalSection(
            projectId, type, request.assigneeUserId(), request.completed()
        );
        return ProposalSectionResponse.of(section, assigneeNames(List.of(section)).get(section.getAssigneeUserId()));
    }

    private Map<String, String> assigneeNames(Collection<ProposalSection> sections) {
        List<String> userIds = sections.stream()
            .map(ProposalSection::getAssigneeUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllByStudentNumberIn(userIds).stream()
            .collect(toMap(User::getStudentNumber, User::getName));
    }

    public ProjectApprovalSummaryResponse getApprovalSummary(Long projectId, String userId) {
        Project project = projectQueryService.getProject(projectId);
        teamAccessValidator.validateMembership(project.getTeamId(), userId);
        ApprovalCount count = projectApprovalRepository.countApprovalsByTeamMembers(
            projectId, project.getTeamId(), project.getProposalRevision()
        );
        return ProjectApprovalSummaryResponse.of((int) count.approvedMembers(), (int) count.totalMembers());
    }

    // screenConfiguration은 [{title, description, imageFileId}, ...]를 원본 그대로 저장·응답하는데,
    // imageFileId만 내려주면 프론트가 그 이미지를 띄울 방법이 없다(presigned URL을 받는 경로가 없음,
    // hoTan35 리뷰). 조회할 때마다 imageFileId를 presigned URL(imageUrl)로 보강해서 내려준다 —
    // 저장은 안 건드린다(15분 후 만료되는 임시 URL이라 영구 저장하면 안 됨).
    //
    // 소유권을 저장 시점에만 확인하고 끝내면, 그 뒤 뭔가의 이유로 저장된 값이 오염돼도 조회할 때마다
    // 계속 URL이 나가버린다. 그래서 여기서도 소유권을 다시 확인하고, 지금 시점에 소유가 아니면
    // URL을 만들지 않는다. "imageUrl"은 항상 먼저 지우고 다시 계산한다 — 클라이언트가 보낸 값이든
    // 과거에 잘못 저장된 값이든 그대로 흘려보내지 않기 위해서다.
    private JsonNode prepareScreenImages(Long teamId, JsonNode screens, Map<Long, String> imageStorageKeys) {
        if (screens == null || !screens.isArray()) {
            return screens;
        }
        Set<String> memberIds = activeMemberIds(teamId);

        // 화면 이미지 ID들을 수집하여 일괄 조회 (N+1 쿼리 방지)
        List<Long> imageFileIds = new java.util.ArrayList<>();
        for (JsonNode screen : screens) {
            JsonNode imageFileId = screen.get("imageFileId");
            if (imageFileId != null && imageFileId.isIntegralNumber()) {
                imageFileIds.add(imageFileId.asLong());
            }
        }

        Map<Long, FileObject> fileObjectMap = java.util.Collections.emptyMap();
        if (!imageFileIds.isEmpty()) {
            fileObjectMap = fileObjectRepository.findAllByIdAndDeletedAtIsNull(imageFileIds.stream().distinct().toList()).stream()
                .collect(java.util.stream.Collectors.toMap(
                    kgu.developers.domain.fileobject.domain.FileObject::getId,
                    java.util.function.Function.identity()
                ));
        }

        ArrayNode resolved = JsonNodeFactory.instance.arrayNode();
        for (JsonNode screen : screens) {
            if (!screen.isObject()) {
                resolved.add(screen);
                continue;
            }
            ObjectNode sanitized = (ObjectNode) screen.deepCopy();
            sanitized.remove("imageUrl");
            JsonNode imageFileId = screen.get("imageFileId");
            if (imageFileId != null && imageFileId.isIntegralNumber()) {
                FileObject fileObject = fileObjectMap.get(imageFileId.asLong());
                if (fileObject != null && memberIds.contains(fileObject.getUploadedBy()) && isAllowedImage(fileObject)) {
                    imageStorageKeys.put(fileObject.getId(), fileObject.getStorageKey());
                }
            }
            resolved.add(sanitized);
        }
        return resolved;
    }

    // 발표자료(SubmissionFacade)는 Submission→Version→Artifact라는 불변 관계로 소유권을 판정하지만,
    // 프로젝트 제안서에는 FileObject와 팀을 잇는 관계가 uploadedBy밖에 없다. 그래서 "업로더가 지금
    // 이 팀의 활성 멤버인가"로 판정한다 — 팀을 옮긴 사람이 올린 이미지는 그 시점부터 URL이 안 나간다.
    // ponytail: 팀 이동 시 기존 이미지가 끊기는 게 문제가 되면 project_screen_image 소유권 테이블로 올린다.
    private void validateScreenImagesOwnedByTeam(Long teamId, JsonNode screens) {
        if (screens == null || !screens.isArray()) {
            return;
        }
        Set<String> memberIds = activeMemberIds(teamId);

        // 화면 이미지 ID들을 수집하여 일괄 조회 (N+1 쿼리 방지)
        List<Long> imageFileIds = new java.util.ArrayList<>();
        for (JsonNode screen : screens) {
            JsonNode imageFileId = screen.get("imageFileId");
            if (imageFileId != null && imageFileId.isIntegralNumber()) {
                imageFileIds.add(imageFileId.asLong());
            }
        }

        Map<Long, FileObject> fileObjectMap = java.util.Collections.emptyMap();
        if (!imageFileIds.isEmpty()) {
            fileObjectMap = fileObjectRepository.findAllByIdAndDeletedAtIsNull(imageFileIds.stream().distinct().toList()).stream()
                .collect(java.util.stream.Collectors.toMap(
                    kgu.developers.domain.fileobject.domain.FileObject::getId,
                    java.util.function.Function.identity()
                ));
        }

        for (JsonNode screen : screens) {
            JsonNode imageFileId = screen.get("imageFileId");
            if (imageFileId == null || imageFileId.isNull()) {
                continue;
            }
            FileObject fileObject = fileObjectMap.get(imageFileId.asLong());
            boolean validScreenImage = fileObject != null
                && memberIds.contains(fileObject.getUploadedBy())
                && isAllowedImage(fileObject);
            if (!validScreenImage) {
                if (fileObject != null && memberIds.contains(fileObject.getUploadedBy())) {
                    throw new FileObjectInvalidTypeException();
                }
                throw new ProjectScreenImageOwnershipException();
            }
        }
    }

    private boolean isAllowedImage(FileObject fileObject) {
        return "image/jpeg".equals(fileObject.getContentType())
            || "image/png".equals(fileObject.getContentType())
            || "image/gif".equals(fileObject.getContentType())
            || "image/bmp".equals(fileObject.getContentType());
    }

    // presigned URL은 조회 시점에 서버가 매번 새로 만드는 값이라 저장하면 안 되는데, 클라이언트가
    // 요청 JSON에 임의의 "imageUrl"을 넣어 보내면 그게 그대로 jsonb에 남는다. 저장 전에 걸러낸다.
    private JsonNode stripClientProvidedImageUrls(JsonNode screens) {
        if (screens == null || !screens.isArray()) {
            return screens;
        }
        ArrayNode sanitized = JsonNodeFactory.instance.arrayNode();
        for (JsonNode screen : screens) {
            if (!screen.isObject()) {
                sanitized.add(screen);
                continue;
            }
            ObjectNode copy = (ObjectNode) screen.deepCopy();
            copy.remove("imageUrl");
            sanitized.add(copy);
        }
        return sanitized;
    }

    private Set<String> activeMemberIds(Long teamId) {
        return teamMemberRepository.findAllByTeamId(teamId).stream()
            .map(TeamMember::getUserId)
            .collect(Collectors.toSet());
    }
}
