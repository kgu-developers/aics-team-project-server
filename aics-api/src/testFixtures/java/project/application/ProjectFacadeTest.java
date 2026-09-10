package project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import kgu.developers.api.project.application.ProjectFacade;
import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.domain.fileobject.exception.FileObjectInvalidTypeException;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.team.application.TeamFacade;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest.MemberRole;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.application.query.ProjectQueryService;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.exception.ProjectScreenImageOwnershipException;
import kgu.developers.domain.projectApproval.domain.ApprovalCount;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.projectApproval.application.command.ProjectApprovalCommandService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.project.domain.ProposalSection;
import kgu.developers.domain.project.domain.ProposalSectionRepository;
import kgu.developers.domain.project.domain.ProposalSectionType;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.api.project.presentation.request.ProposalSectionRequest;
import kgu.developers.common.json.JsonConverter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProjectFacadeTest {

    private static final Long TEAM_ID = 1L;
    private static final String MEMBER_ID = "202412345";

    @Mock private ProjectCommandService projectCommandService;
    @Mock private ProjectQueryService projectQueryService;
    @Mock private TeamAccessValidator teamAccessValidator;
    @Mock private ProjectApprovalRepository projectApprovalRepository;
    @Mock private ProjectApprovalCommandService projectApprovalCommandService;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private FileObjectRepository fileObjectRepository;
    @Mock private FileStorage fileStorage;
    @Mock private ProposalSectionRepository proposalSectionRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamFacade teamFacade;
    @InjectMocks private ProjectFacade projectFacade;

    @Test
    @DisplayName("getProject는 팀원과 담당 교수에게 프로젝트 제안서를 반환한다")
    void getProject() {
        given(projectQueryService.getProjectByTeamId(TEAM_ID)).willReturn(project());

        assertThat(projectFacade.getProject(TEAM_ID, MEMBER_ID).title()).isEqualTo("AI 학습 도우미");
    }

    @Test
    @DisplayName("getProject는 팀원도 담당 교수도 아니면 접근을 거부한다")
    void getProject_deniesOutsider() {
        org.mockito.BDDMockito.willThrow(new AccessDeniedException("접근 거부"))
            .given(teamAccessValidator).validateMembershipOrProfessor(TEAM_ID, MEMBER_ID);

        assertThatThrownBy(() -> projectFacade.getProject(TEAM_ID, MEMBER_ID))
            .isInstanceOf(AccessDeniedException.class);
        then(projectQueryService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("saveProject는 팀원이 아니면 접근을 거부한다")
    void saveProject_deniesNonMember() throws Exception {
        org.mockito.BDDMockito.willThrow(new AccessDeniedException("접근 거부"))
            .given(teamAccessValidator).validateMembership(TEAM_ID, MEMBER_ID);

        assertThatThrownBy(() -> projectFacade.saveProject(TEAM_ID, MEMBER_ID, request()))
            .isInstanceOf(AccessDeniedException.class);
        then(projectCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("uploadProjectImage는 이미지 파일만 저장하고 파일 식별자를 반환한다")
    void uploadProjectImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "screen.png", "image/png", png());
        given(fileStorage.upload(image, "image/png", "screen.png")).willReturn("projects/screen.png");
        given(fileStorage.presignedUrl("projects/screen.png")).willReturn("https://s3/presigned");
        given(fileObjectRepository.save(org.mockito.ArgumentMatchers.any(FileObject.class))).willAnswer(invocation -> {
            FileObject file = invocation.getArgument(0);
            return FileObject.builder()
                .id(42L)
                .uploadedBy(file.getUploadedBy())
                .storageKey(file.getStorageKey())
                .fileName(file.getFileName())
                .contentType(file.getContentType())
                .size(file.getSize())
                .build();
        });

        var response = projectFacade.uploadProjectImage(TEAM_ID, MEMBER_ID, image);

        assertThat(response.fileId()).isEqualTo(42L);
        assertThat(response.imageUrl()).isEqualTo("https://s3/presigned");
        then(teamAccessValidator).should().validateMembership(TEAM_ID, MEMBER_ID);
        then(fileStorage).should().upload(image, "image/png", "screen.png");
        then(fileStorage).should().presignedUrl("projects/screen.png");
        then(fileObjectRepository).should().save(org.mockito.ArgumentMatchers.argThat(file ->
            file.getUploadedBy().equals(MEMBER_ID)
                && file.getFileName().equals("screen.png")
                && file.getContentType().equals("image/png")
                && file.getSize() == image.getSize()
        ));
        then(projectCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("uploadProjectImage는 실제 PNG면 multipart MIME이 octet-stream이어도 저장한다")
    void uploadProjectImage_acceptsImageWithGenericMimeType() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "screen.png", "application/octet-stream", png());
        given(fileStorage.upload(image, "image/png", "screen.png")).willReturn("projects/screen.png");
        given(fileObjectRepository.save(org.mockito.ArgumentMatchers.any(FileObject.class))).willAnswer(invocation -> {
            FileObject file = invocation.getArgument(0);
            return FileObject.builder().id(42L).storageKey(file.getStorageKey()).build();
        });
        given(fileStorage.presignedUrl("projects/screen.png")).willReturn("https://s3/presigned");

        projectFacade.uploadProjectImage(TEAM_ID, MEMBER_ID, image);

        then(fileObjectRepository).should().save(org.mockito.ArgumentMatchers.argThat(file ->
            file.getContentType().equals("image/png")
        ));
    }

    @Test
    @DisplayName("uploadProjectImage는 이미지가 아닌 파일을 저장하지 않는다")
    void uploadProjectImage_rejectsNonImage() {
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> projectFacade.uploadProjectImage(TEAM_ID, MEMBER_ID, file))
            .isInstanceOf(FileObjectInvalidTypeException.class);

        then(fileStorage).shouldHaveNoInteractions();
        then(fileObjectRepository).shouldHaveNoInteractions();
        then(projectCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("uploadProjectImage는 이미지 Content-Type으로 위장한 파일을 저장하지 않는다")
    void uploadProjectImage_rejectsInvalidImageContent() {
        MockMultipartFile file = new MockMultipartFile("file", "malware.png", "image/png", "not an image".getBytes());

        assertThatThrownBy(() -> projectFacade.uploadProjectImage(TEAM_ID, MEMBER_ID, file))
            .isInstanceOf(FileObjectInvalidTypeException.class);

        then(fileStorage).shouldHaveNoInteractions();
        then(fileObjectRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("saveProject는 요청 필드를 커맨드 서비스에 전달한다")
    void saveProject() throws Exception {
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateMembership(TEAM_ID, MEMBER_ID);
        givenImageUploadedBy(MEMBER_ID);
        given(projectCommandService.saveProject(org.mockito.ArgumentMatchers.eq(TEAM_ID), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willReturn(project());

        assertThat(projectFacade.saveProject(TEAM_ID, MEMBER_ID, request()).goal()).isEqualTo("피드백 자동화");
    }

    @Test
    @DisplayName("saveProject는 우리 팀이 올리지 않은 imageFileId를 거부한다")
    void saveProject_rejectsForeignImage() throws Exception {
        givenImageUploadedBy("999999999");

        assertThatThrownBy(() -> projectFacade.saveProject(TEAM_ID, MEMBER_ID, request()))
            .isInstanceOf(ProjectScreenImageOwnershipException.class);
        then(projectCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("uploadProjectImage는 가로 또는 세로가 4096px를 초과하면 저장하지 않는다")
    void uploadProjectImage_rejectsImageExceedingDimensions() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "wide.png", "image/png", png(4097, 1));

        assertThatThrownBy(() -> projectFacade.uploadProjectImage(TEAM_ID, MEMBER_ID, image))
            .isInstanceOf(FileObjectInvalidTypeException.class);

        then(fileStorage).shouldHaveNoInteractions();
        then(fileObjectRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("uploadProjectImage는 총 픽셀 수가 16,000,000px를 초과하면 저장하지 않는다")
    void uploadProjectImage_rejectsImageExceedingPixelLimit() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "large.png", "image/png", png(4000, 4001));

        assertThatThrownBy(() -> projectFacade.uploadProjectImage(TEAM_ID, MEMBER_ID, image))
            .isInstanceOf(FileObjectInvalidTypeException.class);

        then(fileStorage).shouldHaveNoInteractions();
        then(fileObjectRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("uploadProjectImage는 DB 저장이 실패하면 업로드한 파일을 삭제한다")
    void uploadProjectImage_deletesUploadedFileWhenSaveFails() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "screen.png", "image/png", png());
        given(fileStorage.upload(image, "image/png", "screen.png")).willReturn("projects/screen.png");
        given(fileObjectRepository.save(org.mockito.ArgumentMatchers.any(FileObject.class)))
            .willThrow(new IllegalStateException("DB 저장 실패"));

        assertThatThrownBy(() -> projectFacade.uploadProjectImage(TEAM_ID, MEMBER_ID, image))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("DB 저장 실패");

        then(fileStorage).should().delete("projects/screen.png");
    }

    @Test
    @DisplayName("uploadProjectImage는 원본 파일명이 없으면 image를 저장한다")
    void uploadProjectImage_usesDefaultFileName() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", null, "image/png", png());
        given(fileStorage.upload(image, "image/png", "image")).willReturn("projects/image");
        given(fileObjectRepository.save(org.mockito.ArgumentMatchers.any(FileObject.class))).willAnswer(invocation ->
            FileObject.builder().id(42L).storageKey(invocation.<FileObject>getArgument(0).getStorageKey()).build());
        given(fileStorage.presignedUrl("projects/image")).willReturn("https://s3/presigned");

        projectFacade.uploadProjectImage(TEAM_ID, MEMBER_ID, image);

        then(fileObjectRepository).should().save(org.mockito.ArgumentMatchers.argThat(file -> file.getFileName().equals("image")));
    }

    @Test
    @DisplayName("uploadProjectImage는 255자를 넘는 원본 파일명을 잘라 저장한다")
    void uploadProjectImage_truncatesLongFileName() throws Exception {
        String fileName = "a".repeat(256);
        String normalized = "a".repeat(255);
        MockMultipartFile image = new MockMultipartFile("file", fileName, "image/png", png());
        given(fileStorage.upload(image, "image/png", normalized)).willReturn("projects/image");
        given(fileObjectRepository.save(org.mockito.ArgumentMatchers.any(FileObject.class))).willAnswer(invocation ->
            FileObject.builder().id(42L).storageKey(invocation.<FileObject>getArgument(0).getStorageKey()).build());
        given(fileStorage.presignedUrl("projects/image")).willReturn("https://s3/presigned");

        projectFacade.uploadProjectImage(TEAM_ID, MEMBER_ID, image);

        then(fileObjectRepository).should().save(org.mockito.ArgumentMatchers.argThat(file -> file.getFileName().equals(normalized)));
    }

    @Test
    @DisplayName("saveProject는 우리 팀원이 올린 PDF를 화면 이미지로 연결하지 않는다")
    void saveProject_rejectsNonImageFile() throws Exception {
        givenImageUploadedBy(MEMBER_ID, "application/pdf");

        assertThatThrownBy(() -> projectFacade.saveProject(TEAM_ID, MEMBER_ID, request()))
            .isInstanceOf(FileObjectInvalidTypeException.class);
        then(projectCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("saveProject는 클라이언트가 보낸 imageUrl을 저장 전에 지운다")
    void saveProject_stripsClientImageUrl() throws Exception {
        givenImageUploadedBy(MEMBER_ID);
        given(projectCommandService.saveProject(org.mockito.ArgumentMatchers.eq(TEAM_ID), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willReturn(project());
        ProjectRequest request = new ProjectRequest("AI 학습 도우미", "설명", "피드백 자동화",
            new ObjectMapper().readTree("[]"),
            new ObjectMapper().readTree("[{\"title\":\"홈\",\"imageFileId\":1,\"imageUrl\":\"https://evil/forever\"}]"),
            null, null, null, null, null, new ObjectMapper().readTree("[]"));

        projectFacade.saveProject(TEAM_ID, MEMBER_ID, request);

        var saved = org.mockito.ArgumentCaptor.forClass(com.fasterxml.jackson.databind.JsonNode.class);
        then(projectCommandService).should().saveProject(org.mockito.ArgumentMatchers.eq(TEAM_ID),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), saved.capture(),
            org.mockito.ArgumentMatchers.any());
        assertThat(saved.getValue().get(0).has("imageUrl")).isFalse();
    }

    @Test
    @DisplayName("getProject는 우리 팀이 올린 화면 이미지에 presigned URL을 채워 내려준다")
    void getProject_fillsImageUrl() throws Exception {
        given(projectQueryService.getProjectByTeamId(TEAM_ID)).willReturn(projectWithScreens(
            "[{\"title\":\"홈\",\"imageFileId\":1,\"imageUrl\":\"https://stale/url\"}]"));
        givenImageUploadedBy(MEMBER_ID);
        given(fileStorage.presignedUrl("teams/1/home.png")).willReturn("https://s3/presigned");

        var screens = projectFacade.getProject(TEAM_ID, MEMBER_ID).screenConfiguration();

        assertThat(screens.get(0).get("imageUrl").asText()).isEqualTo("https://s3/presigned");
    }

    @Test
    @DisplayName("getProject는 업로더가 더 이상 팀원이 아니면 imageUrl을 만들지 않는다")
    void getProject_dropsImageUrlWhenUploaderLeftTeam() throws Exception {
        given(projectQueryService.getProjectByTeamId(TEAM_ID)).willReturn(projectWithScreens(
            "[{\"title\":\"홈\",\"imageFileId\":1,\"imageUrl\":\"https://stale/url\"}]"));
        givenImageUploadedBy("999999999");

        var screens = projectFacade.getProject(TEAM_ID, MEMBER_ID).screenConfiguration();

        assertThat(screens.get(0).has("imageUrl")).isFalse();
        assertThat(screens.get(0).get("imageFileId").asLong()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getProject는 PDF로 저장된 기존 화면 파일의 URL을 발급하지 않는다")
    void getProject_dropsImageUrlForNonImageFile() throws Exception {
        given(projectQueryService.getProjectByTeamId(TEAM_ID)).willReturn(projectWithScreens(
            "[{\"title\":\"홈\",\"imageFileId\":1,\"imageUrl\":\"https://stale/url\"}]"));
        givenImageUploadedBy(MEMBER_ID, "application/pdf");

        var screens = projectFacade.getProject(TEAM_ID, MEMBER_ID).screenConfiguration();

        assertThat(screens.get(0).has("imageUrl")).isFalse();
        then(fileStorage).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("getProject는 같은 이미지를 여러 화면이 참조해도 정상 처리한다")
    void getProject_handlesDuplicateImageIds() throws Exception {
        given(projectQueryService.getProjectByTeamId(TEAM_ID)).willReturn(projectWithScreens(
            "[{\"title\":\"홈\",\"imageFileId\":1},{\"title\":\"대시보드\",\"imageFileId\":1}]"));
        given(teamMemberRepository.findAllByTeamId(TEAM_ID))
            .willReturn(List.of(TeamMember.create(TEAM_ID, MEMBER_ID, true, "팀장")));
        given(fileObjectRepository.findAllByIdAndDeletedAtIsNull(List.of(1L)))
            .willReturn(List.of(FileObject.builder()
                .id(1L).uploadedBy(MEMBER_ID).storageKey("teams/1/shared.png").contentType("image/png").build()));
        given(fileStorage.presignedUrl("teams/1/shared.png")).willReturn("https://s3/presigned");

        var screens = projectFacade.getProject(TEAM_ID, MEMBER_ID).screenConfiguration();

        assertThat(screens).hasSize(2);
        assertThat(screens.get(0).get("imageUrl").asText()).isEqualTo("https://s3/presigned");
        assertThat(screens.get(1).get("imageUrl").asText()).isEqualTo("https://s3/presigned");
        then(fileObjectRepository).should().findAllByIdAndDeletedAtIsNull(List.of(1L));
    }

    private void givenImageUploadedBy(String uploaderId) {
        givenImageUploadedBy(uploaderId, "image/png");
    }

    private void givenImageUploadedBy(String uploaderId, String contentType) {
        given(teamMemberRepository.findAllByTeamId(TEAM_ID))
            .willReturn(List.of(TeamMember.create(TEAM_ID, MEMBER_ID, true, "팀장")));
        // 화면 이미지는 N+1을 피하려고 한 번에 조회한다(ProjectFacade.resolveScreenImageUrls).
        given(fileObjectRepository.findAllByIdAndDeletedAtIsNull(List.of(1L))).willReturn(List.of(FileObject.builder()
            .id(1L).uploadedBy(uploaderId).storageKey("teams/1/home.png").contentType(contentType).build()));
    }

    @Test
    @DisplayName("completeProposal은 팀장이면 완료 처리한다")
    void completeProposal() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateLeader(TEAM_ID, MEMBER_ID);

        projectFacade.completeProposal(10L, MEMBER_ID);

        then(projectCommandService).should().completeProposal(10L);
    }

    @Test
    @DisplayName("completeProposal은 팀 행을 잠근 뒤 팀장 권한을 확인한다")
    void completeProposal_locksTeamBeforeValidation() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateLeader(TEAM_ID, MEMBER_ID);

        projectFacade.completeProposal(10L, MEMBER_ID);

        InOrder inOrder = org.mockito.Mockito.inOrder(projectCommandService, teamAccessValidator);
        inOrder.verify(projectCommandService).lockTeam(TEAM_ID);
        inOrder.verify(teamAccessValidator).validateLeader(TEAM_ID, MEMBER_ID);
        inOrder.verify(projectCommandService).completeProposal(10L);
    }

    @Test
    @DisplayName("completeProposal은 팀장이 아니면 접근을 거부한다")
    void completeProposal_deniesNonLeader() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willThrow(new AccessDeniedException("접근 거부"))
            .given(teamAccessValidator).validateLeader(TEAM_ID, MEMBER_ID);

        assertThatThrownBy(() -> projectFacade.completeProposal(10L, MEMBER_ID))
            .isInstanceOf(AccessDeniedException.class);
        then(projectCommandService).should(org.mockito.Mockito.never()).completeProposal(10L);
    }

    @Test
    @DisplayName("deleteProject는 팀장이면 제안서를 삭제한다")
    void deleteProject() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateLeader(TEAM_ID, MEMBER_ID);

        projectFacade.deleteProject(10L, MEMBER_ID);

        then(projectCommandService).should().deleteProject(10L);
    }

    @Test
    @DisplayName("deleteProject는 팀장이 아니면 접근을 거부한다")
    void deleteProject_deniesNonLeader() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willThrow(new AccessDeniedException("접근 거부"))
            .given(teamAccessValidator).validateLeader(TEAM_ID, MEMBER_ID);

        assertThatThrownBy(() -> projectFacade.deleteProject(10L, MEMBER_ID))
            .isInstanceOf(AccessDeniedException.class);
        then(projectCommandService).should(org.mockito.Mockito.never()).deleteProject(10L);
    }

    @Test
    @DisplayName("approveProject는 본인 팀원 동의를 저장한다")
    void approveProject() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateMembership(TEAM_ID, MEMBER_ID);

        projectFacade.approveProject(10L, MEMBER_ID);

        then(projectApprovalCommandService).should()
            .approve(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(MEMBER_ID),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("getApprovalSummary는 완료 인원과 전체 인원을 반환한다")
    void getApprovalSummary() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateMembership(TEAM_ID, MEMBER_ID);
        given(projectApprovalRepository.countApprovalsByTeamMembers(10L, TEAM_ID, 0L))
            .willReturn(new ApprovalCount(2L, 1L));

        var response = projectFacade.getApprovalSummary(10L, MEMBER_ID);

        assertThat(response.approvedCount()).isEqualTo(1);
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.progress()).isEqualTo("1/2");
    }

    @Test
    @DisplayName("getProposalSections는 저장된 행이 없는 섹션까지 고정 구성 전체를 내려준다")
    void getProposalSections() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        ProposalSection screen = ProposalSection.create(10L, ProposalSectionType.SCREEN);
        screen.assign(MEMBER_ID);
        screen.updateCompleted(true);
        given(proposalSectionRepository.findAllByProjectId(10L)).willReturn(List.of(screen));
        given(userRepository.findAllByStudentNumberIn(List.of(MEMBER_ID))).willReturn(List.of(member()));

        var response = projectFacade.getProposalSections(10L, MEMBER_ID);

        assertThat(response.contents()).hasSize(ProposalSectionType.values().length);
        assertThat(response.allCompleted()).isFalse();
        assertThat(response.contents()).anySatisfy(section -> {
            assertThat(section.section()).isEqualTo(ProposalSectionType.SCREEN);
            assertThat(section.assigneeName()).isEqualTo("홍길동");
            assertThat(section.completed()).isTrue();
        });
    }

    @Test
    @DisplayName("updateProposalSection은 팀원이 아니면 접근을 거부한다")
    void updateProposalSection_deniesNonMember() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willThrow(new AccessDeniedException("접근 거부"))
            .given(teamAccessValidator).validateMembership(TEAM_ID, MEMBER_ID);

        assertThatThrownBy(() -> projectFacade.updateProposalSection(
            10L, ProposalSectionType.SCREEN, MEMBER_ID, new ProposalSectionRequest(MEMBER_ID, true)))
            .isInstanceOf(AccessDeniedException.class);
        then(projectCommandService).should(org.mockito.Mockito.never())
            .updateProposalSection(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    // 제안서 5번 본문은 킥오프와 저장소가 같아서 Team·team_member로 흘러가야 한다.
    @Test
    @DisplayName("saveProject는 팀 운영방식 본문을 킥오프 저장소에 반영한다")
    void saveProject_writesTeamOperationToKickoff() throws Exception {
        givenImageUploadedBy(MEMBER_ID);
        given(projectCommandService.saveProject(org.mockito.ArgumentMatchers.eq(TEAM_ID), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willReturn(project());
        List<MemberRole> roles = List.of(new MemberRole(MEMBER_ID, "백엔드"));
        ProjectRequest request = new ProjectRequest("AI 학습 도우미", "설명", "피드백 자동화",
            JsonConverter.parse("[]"), new ObjectMapper().readTree("[{\"title\":\"홈\",\"imageFileId\":1}]"),
            "매주 화요일 회고", "매주 목 19:00 온라인", roles, "4월: 설계", null, null);

        projectFacade.saveProject(TEAM_ID, MEMBER_ID, request);

        then(teamFacade).should().updateKickoffContent(
            TEAM_ID, MEMBER_ID, "매주 화요일 회고", "매주 목 19:00 온라인", roles);
    }

    @Test
    @DisplayName("saveProject는 팀 운영방식을 넘기지 않으면 킥오프를 건드리지 않는다")
    void saveProject_keepsKickoffWhenTeamOperationOmitted() throws Exception {
        givenImageUploadedBy(MEMBER_ID);
        given(projectCommandService.saveProject(org.mockito.ArgumentMatchers.eq(TEAM_ID), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willReturn(project());

        projectFacade.saveProject(TEAM_ID, MEMBER_ID, request());

        // 응답에 5번 본문을 담느라 킥오프를 읽기는 한다. 쓰지 않는 것만 확인한다.
        then(teamFacade).should(org.mockito.Mockito.never()).updateKickoffContent(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    private User member() {
        return User.create(MEMBER_ID, "member@kgu.ac.kr", "홍길동", "password", UserGlobalRole.USER, "01000000000");
    }

    private byte[] png() throws Exception {
        return png(1, 1);
    }

    private byte[] png(int width, int height) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", output);
        return output.toByteArray();
    }

    private ProjectRequest request() throws Exception {
        return new ProjectRequest("AI 학습 도우미", "설명", "피드백 자동화",
            JsonConverter.parse("[{\"name\":\"학습 로그\",\"description\":\"문제 풀이 기록\",\"expectedCount\":\"약 1만 건\"}]"),
            new ObjectMapper().readTree("[{\"title\":\"홈\",\"imageFileId\":1}]"),
            null, null, null, "4월: 설계, 5월: 개발", "https://github.com/kgu/project", new ObjectMapper().readTree("[]"));
    }

    private Project project() {
        return Project.builder().id(10L).teamId(TEAM_ID).title("AI 학습 도우미").description("설명")
            .goal("피드백 자동화").approvalStatus(ApprovalStatus.DRAFT).build();
    }

    private Project projectWithScreens(String screensJson) throws Exception {
        return Project.builder().id(10L).teamId(TEAM_ID).title("AI 학습 도우미").description("설명")
            .goal("피드백 자동화").approvalStatus(ApprovalStatus.DRAFT)
            .screenConfiguration(new ObjectMapper().readTree(screensJson)).build();
    }
}
