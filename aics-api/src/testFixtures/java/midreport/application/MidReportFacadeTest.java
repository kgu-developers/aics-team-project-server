package midreport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kgu.developers.api.midreport.application.MidReportFacade;
import kgu.developers.api.midreport.presentation.request.MidReportBlockUpdateRequest;
import kgu.developers.api.midreport.presentation.request.MidReportSubmissionRequest;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.midreport.application.command.MidReportCommandService;
import kgu.developers.domain.midreport.application.query.MidReportQueryService;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.exception.MidReportLeaderOnlyException;
import kgu.developers.domain.midreport.exception.MidReportGuiImageOwnershipException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class MidReportFacadeTest {
    private static final String USER_ID = "202600001";
    private static final Long TEAM_ID = 10L;
    private static final Long SECTION_ID = 20L;
    private static final Long MILESTONE_ID = 30L;

    @Mock private MidReportCommandService midReportCommandService;
    @Mock private MidReportQueryService midReportQueryService;
    @Mock private TeamAccessValidator teamAccessValidator;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private MilestoneRepository milestoneRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserQueryService userQueryService;
    @Mock private FileObjectRepository fileObjectRepository;
    @Mock private FileStorage fileStorage;
    @InjectMocks private MidReportFacade midReportFacade;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void allowUserRole() {
        given(userQueryService.getUserByStudentNumber(USER_ID)).willReturn(user(UserGlobalRole.USER, "학생 A"));
    }

    @Test
    @DisplayName("현재 중간보고서는 활성 학생의 팀과 게시된 MID_REPORT 마일스톤으로 생성한다")
    void getsOrCreatesCurrentReport() {
        Enrollment enrollment = enrollment(Role.STUDENT);
        TeamMember member = TeamMember.builder().id(1L).teamId(TEAM_ID).userId(USER_ID).isLeader(true).build();
        Milestone milestone = midReportMilestone();
        Project project = Project.builder().id(40L).teamId(TEAM_ID).title("CineFlow").description("영화관 관리")
            .goal("목표").approvalStatus(ApprovalStatus.DRAFT).build();
        given(enrollmentRepository.findAllByUserId(USER_ID)).willReturn(List.of(enrollment));
        given(teamMemberRepository.findActiveBySectionIdAndUserId(SECTION_ID, USER_ID)).willReturn(Optional.of(member));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(milestone));
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team()));
        given(projectRepository.findAllByTeamId(TEAM_ID)).willReturn(List.of(project));
        given(midReportCommandService.getOrCreate(
            eq(TEAM_ID), eq(MILESTONE_ID), eq("CineFlow 중간보고서"), any(), eq("CineFlow"), eq("영화관 관리")
        )).willReturn(report());
        given(teamMemberRepository.findLeaderByTeamId(TEAM_ID)).willReturn(Optional.of(member));
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(any())).willReturn(List.of(user(UserGlobalRole.USER, "학생 A")));

        var response = midReportFacade.getCurrent(USER_ID);

        assertThat(response.teamId()).isEqualTo(TEAM_ID);
        assertThat(response.teamLeaderName()).isEqualTo("학생 A");
        assertThat(response.blocks()).hasSize(4);
        assertThat(response.blocks()).allSatisfy(block -> {
            assertThat(block.lastEditedBy()).isNotNull();
            assertThat(block.lastSavedAt()).isNotNull();
        });
        then(teamAccessValidator).should().validateMembership(TEAM_ID, USER_ID);
    }

    @Test
    @DisplayName("ASSISTANT 수강 역할은 현재 중간보고서 접근을 거부한다")
    void deniesAssistantEnrollment() {
        given(enrollmentRepository.findAllByUserId(USER_ID)).willReturn(List.of(enrollment(Role.ASSISTANT)));

        assertThatThrownBy(() -> midReportFacade.getCurrent(USER_ID))
            .isInstanceOf(AccessDeniedException.class);
        then(midReportCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("ADMIN 전역 역할은 학생 팀원이더라도 중간보고서 접근을 거부한다")
    void deniesAdminGlobalRole() {
        given(userQueryService.getUserByStudentNumber(USER_ID)).willReturn(user(UserGlobalRole.ADMIN, "관리자"));

        assertThatThrownBy(() -> midReportFacade.getCurrent(USER_ID))
            .isInstanceOf(AccessDeniedException.class);
        then(enrollmentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("영역 저장은 팀 소속과 활성 STUDENT 수강 상태를 모두 검증한다")
    void updateRequiresActiveStudentMembership() throws Exception {
        LocalDateTime updatedDueDate = LocalDateTime.of(2026, 11, 2, 23, 59);
        given(midReportQueryService.getById(100L)).willReturn(report());
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team()));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
            .willReturn(Optional.of(enrollment(Role.STUDENT)));
        given(midReportCommandService.updateBlock(eq(100L), eq("topic"), eq(0L), any(), eq(USER_ID), any()))
            .willReturn(report());
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(midReportMilestone(updatedDueDate)));
        given(teamMemberRepository.findLeaderByTeamId(TEAM_ID)).willReturn(Optional.empty());
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(any())).willReturn(List.of());
        MidReportBlockUpdateRequest request = new MidReportBlockUpdateRequest(0L, objectMapper.readTree("""
            [{"key":"title","value":"CineFlow"},{"key":"description","value":"설명"}]
            """));

        var response = midReportFacade.updateBlock(100L, "topic", USER_ID, request);

        assertThat(response.dueDate()).isEqualTo(updatedDueDate);
        then(teamAccessValidator).should().validateMembership(TEAM_ID, USER_ID);
        then(midReportCommandService).should().updateBlock(eq(100L), eq("topic"), eq(0L), any(), eq(USER_ID), any());
    }

    @Test
    @DisplayName("영역 저장은 ASSISTANT 수강 역할을 거부한다")
    void updateRejectsAssistantEnrollment() throws Exception {
        given(midReportQueryService.getById(100L)).willReturn(report());
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team()));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
            .willReturn(Optional.of(enrollment(Role.ASSISTANT)));
        MidReportBlockUpdateRequest request = new MidReportBlockUpdateRequest(0L, objectMapper.readTree("[]"));

        assertThatThrownBy(() -> midReportFacade.updateBlock(100L, "topic", USER_ID, request))
            .isInstanceOf(AccessDeniedException.class);

        then(teamAccessValidator).should().validateMembership(TEAM_ID, USER_ID);
        then(midReportCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("활성 학생인 팀장은 팀원 승인 없이 중간보고서를 최종 제출한다")
    void leaderSubmitsReport() {
        TeamMember leader = TeamMember.builder()
            .id(1L).teamId(TEAM_ID).userId(USER_ID).isLeader(true).build();
        given(midReportQueryService.getById(100L)).willReturn(report());
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team()));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
            .willReturn(Optional.of(enrollment(Role.STUDENT)));
        given(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID)).willReturn(Optional.of(leader));
        given(midReportCommandService.submit(eq(100L), eq(0L), eq(USER_ID), any())).willReturn(report());
        given(teamMemberRepository.findLeaderByTeamId(TEAM_ID)).willReturn(Optional.of(leader));
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(any()))
            .willReturn(List.of(user(UserGlobalRole.USER, "학생 A")));

        midReportFacade.submit(100L, USER_ID, new MidReportSubmissionRequest(0L));

        then(midReportCommandService).should().submit(eq(100L), eq(0L), eq(USER_ID), any());
    }

    @Test
    @DisplayName("활성 팀원이더라도 팀장이 아니면 최종 제출할 수 없다")
    void memberCannotSubmitReport() {
        TeamMember member = TeamMember.builder()
            .id(1L).teamId(TEAM_ID).userId(USER_ID).isLeader(false).build();
        given(midReportQueryService.getById(100L)).willReturn(report());
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team()));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
            .willReturn(Optional.of(enrollment(Role.STUDENT)));
        given(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> midReportFacade.submit(
            100L, USER_ID, new MidReportSubmissionRequest(0L)
        )).isInstanceOf(MidReportLeaderOnlyException.class);

        then(midReportCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GUI 화면 이미지는 현재 팀원의 파일만 저장하고 조회 URL을 보강한다")
    void storesAndResolvesTeamGuiImage() throws Exception {
        MidReport report = report();
        TeamMember member = TeamMember.builder().id(1L).teamId(TEAM_ID).userId(USER_ID).isLeader(true).build();
        FileObject file = imageFile(50L, USER_ID, null);
        given(midReportQueryService.getById(100L)).willReturn(report);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team()));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
            .willReturn(Optional.of(enrollment(Role.STUDENT)));
        given(teamMemberRepository.findAllByTeamId(TEAM_ID)).willReturn(List.of(member));
        given(fileObjectRepository.findById(50L)).willReturn(Optional.of(file));
        given(fileStorage.presignedUrl("mid-report/home.png")).willReturn("https://files.example/home.png");
        given(midReportCommandService.updateBlock(eq(100L), eq("gui-design"), eq(0L), any(), eq(USER_ID), any()))
            .willAnswer(invocation -> {
                report.updateBlock("gui-design", 0L, invocation.getArgument(3), USER_ID, LocalDateTime.now());
                return report;
            });
        given(teamMemberRepository.findLeaderByTeamId(TEAM_ID)).willReturn(Optional.of(member));
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(any()))
            .willReturn(List.of(user(UserGlobalRole.USER, "학생 A")));

        MidReportBlockUpdateRequest request = guiRequest("""
            [{"id":"screen-1","name":"홈","description":"홈 화면","imageFileId":50,"imageName":"임의.png","imageUrl":"https://client.example/forged"}]
            """);

        var response = midReportFacade.updateBlock(100L, "gui-design", USER_ID, request);

        String storedScreens = fieldValue(report, "gui-design", "guiScreens");
        assertThat(storedScreens).contains("\"imageName\":\"home.png\"");
        assertThat(storedScreens).doesNotContain("imageUrl");
        String responseScreens = response.blocks().stream()
            .filter(block -> block.key().equals("gui-design"))
            .map(block -> fieldValue(block.fields(), "guiScreens"))
            .findFirst()
            .orElseThrow();
        assertThat(responseScreens).contains("\"imageUrl\":\"https://files.example/home.png\"");
    }

    @Test
    @DisplayName("GUI 화면 이미지는 다른 팀원이 올린 파일을 거부한다")
    void rejectsGuiImageUploadedByAnotherTeam() throws Exception {
        MidReport report = report();
        given(midReportQueryService.getById(100L)).willReturn(report);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team()));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
            .willReturn(Optional.of(enrollment(Role.STUDENT)));
        given(teamMemberRepository.findAllByTeamId(TEAM_ID)).willReturn(List.of(
            TeamMember.builder().id(1L).teamId(TEAM_ID).userId(USER_ID).build()
        ));
        given(fileObjectRepository.findById(50L)).willReturn(Optional.of(imageFile(50L, "202699999", null)));

        assertThatThrownBy(() -> midReportFacade.updateBlock(
            100L, "gui-design", USER_ID, guiRequest("""
                [{"id":"screen-1","name":"홈","description":"홈 화면","imageFileId":50}]
                """)
        )).isInstanceOf(MidReportGuiImageOwnershipException.class);

        then(midReportCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GUI 화면 이미지는 삭제된 파일을 거부한다")
    void rejectsDeletedGuiImage() throws Exception {
        MidReport report = report();
        given(midReportQueryService.getById(100L)).willReturn(report);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team()));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
            .willReturn(Optional.of(enrollment(Role.STUDENT)));
        given(teamMemberRepository.findAllByTeamId(TEAM_ID)).willReturn(List.of(
            TeamMember.builder().id(1L).teamId(TEAM_ID).userId(USER_ID).build()
        ));
        given(fileObjectRepository.findById(50L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> midReportFacade.updateBlock(
            100L, "gui-design", USER_ID, guiRequest("""
                [{"id":"screen-1","name":"홈","description":"홈 화면","imageFileId":50}]
                """)
        )).isInstanceOf(MidReportGuiImageOwnershipException.class);
    }

    private Enrollment enrollment(Role role) {
        return Enrollment.builder().id(1L).sectionId(SECTION_ID).userId(USER_ID).role(role).status(Status.ACTIVE).build();
    }

    private Team team() {
        return Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("7팀").build();
    }

    private Milestone midReportMilestone() {
        return midReportMilestone(LocalDateTime.of(2026, 10, 26, 23, 59));
    }

    private Milestone midReportMilestone(LocalDateTime dueDate) {
        Milestone milestone = Milestone.create(
            SECTION_ID,
            "중간보고서",
            "설명",
            8,
            new MilestoneSchedule(null, dueDate, null, null, null, null),
            MilestoneType.MID_REPORT
        );
        milestone.changeStatus(MilestoneStatus.PUBLISHED);
        return Milestone.restore(
            MILESTONE_ID, SECTION_ID, milestone.getTitle(), milestone.getDescription(), milestone.getWeekNumber(),
            milestone.getStatus(), milestone.getSchedule(), milestone.getType(), false
        );
    }

    private MidReport report() {
        MidReport created = MidReport.create(
            TEAM_ID, MILESTONE_ID, "CineFlow 중간보고서", LocalDateTime.of(2026, 10, 26, 23, 59), "CineFlow", "영화관 관리"
        );
        return MidReport.builder()
            .id(100L).teamId(TEAM_ID).milestoneId(MILESTONE_ID).title(created.getTitle()).version(0L)
            .dueDate(created.getDueDate()).status(created.getStatus()).blocks(created.getBlocks())
            .createdAt(LocalDateTime.of(2026, 9, 8, 12, 0)).build();
    }

    private MidReportBlockUpdateRequest guiRequest(String screens) {
        ArrayNode fields = objectMapper.createArrayNode();
        ObjectNode field = fields.addObject();
        field.put("key", "guiScreens");
        field.put("value", screens.strip());
        return new MidReportBlockUpdateRequest(0L, fields);
    }

    private String fieldValue(MidReport report, String blockKey, String fieldKey) {
        return report.getBlocks().stream()
            .filter(block -> block.getKey().equals(blockKey))
            .map(block -> fieldValue(block.getFields(), fieldKey))
            .findFirst()
            .orElseThrow();
    }

    private String fieldValue(com.fasterxml.jackson.databind.JsonNode fields, String fieldKey) {
        for (var field : fields) {
            if (fieldKey.equals(field.path("key").asText())) {
                return field.path("value").asText();
            }
        }
        throw new IllegalArgumentException("필드를 찾을 수 없습니다.");
    }

    private FileObject imageFile(Long id, String uploadedBy, LocalDateTime deletedAt) {
        return FileObject.builder()
            .id(id)
            .uploadedBy(uploadedBy)
            .storageKey("mid-report/home.png")
            .fileName("home.png")
            .contentType("image/png")
            .size(1024L)
            .previewSupported(true)
            .previewType("IMAGE")
            .deletedAt(deletedAt)
            .build();
    }

    private User user(UserGlobalRole role, String name) {
        return User.builder().studentNumber(USER_ID).name(name).globalRole(role).build();
    }
}
