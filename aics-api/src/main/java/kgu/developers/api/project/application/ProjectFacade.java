package kgu.developers.api.project.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.project.presentation.request.ProposalSectionRequest;
import kgu.developers.api.project.presentation.response.ProjectResponse;
import kgu.developers.api.project.presentation.response.ProjectApprovalSummaryResponse;
import kgu.developers.api.project.presentation.response.ProposalSectionListResponse;
import kgu.developers.api.project.presentation.response.ProposalSectionResponse;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.team.application.TeamFacade;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.application.query.ProjectQueryService;
import kgu.developers.domain.project.domain.Project;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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

    private final ProjectCommandService projectCommandService;
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

    public ProjectResponse getProject(Long teamId, String userId) {
        teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
        Project project = projectQueryService.getProjectByTeamId(teamId);
        return ProjectResponse.from(project, resolveScreenImageUrls(teamId, project.getScreenConfiguration()),
            teamFacade.getKickoffByTeamId(teamId, userId));
    }

    public ProjectResponse saveProject(Long teamId, String userId, ProjectRequest request) {
        teamAccessValidator.validateMembership(teamId, userId);
        validateScreenImagesOwnedByTeam(teamId, request.screenConfiguration());

        // 팀 운영방식 본문(팀규칙·회의방식·역할분담)은 킥오프와 저장소가 같아서 Team·team_member에 쓴다.
        // TeamFacade가 감사로그와 동의 무효화까지 같이 처리한다.
        if (request.kickoffRule() != null || request.meetingSchedule() != null || request.memberRoles() != null) {
            teamFacade.updateKickoffContent(teamId, userId,
                request.kickoffRule(), request.meetingSchedule(), request.memberRoles());
        }

        Project project = projectCommandService.saveProject(
            teamId,
            request.title(),
            request.description(),
            request.goal(),
            request.repositoryUrl(),
            request.externalLinks(),
            request.dataConfiguration(),
            stripClientProvidedImageUrls(request.screenConfiguration()),
            request.keyFeatures(),
            request.demoFlow(),
            request.projectSchedule()
        );
        return ProjectResponse.from(project, resolveScreenImageUrls(teamId, project.getScreenConfiguration()),
            teamFacade.getKickoffByTeamId(teamId, userId));
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
    private JsonNode resolveScreenImageUrls(Long teamId, JsonNode screens) {
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
                if (fileObject != null && memberIds.contains(fileObject.getUploadedBy())) {
                    sanitized.put("imageUrl", fileStorage.presignedUrl(fileObject.getStorageKey()));
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
            boolean ownedByTeam = fileObject != null && memberIds.contains(fileObject.getUploadedBy());
            if (!ownedByTeam) {
                throw new ProjectScreenImageOwnershipException();
            }
        }
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
