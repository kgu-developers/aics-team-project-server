package midreport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kgu.developers.admin.midreport.application.MidReportAdminFacade;
import kgu.developers.admin.midreport.presentation.request.MidReportFeedbackAdminRequest;
import kgu.developers.admin.midreport.presentation.response.MidReportAdminResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportFeedbackAdminPageResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportFeedbackAdminResponse;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.midreport.application.command.MidReportCommandService;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportBlock;
import kgu.developers.domain.midreport.domain.MidReportBlockDefinition;
import kgu.developers.domain.midreport.domain.MidReportBlockStatus;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.midreport.domain.MidReportRevision;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import kgu.developers.domain.midreport.exception.MidReportNotFoundException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teammessage.application.command.TeamMessageCommandService;
import kgu.developers.domain.teammessage.application.query.TeamMessageQueryService;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teamthread.application.command.TeamThreadCommandService;
import kgu.developers.domain.teamthread.application.query.TeamThreadQueryService;
import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class MidReportAdminFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final Long TEAM_ID = 10L;
    private static final Long MILESTONE_ID = 100L;
    private static final Long REPORT_ID = 200L;
    private static final String PROFESSOR_ID = "202699999";
    private static final String STUDENT_ID = "202412345";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private SectionQueryService sectionQueryService;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MidReportRepository midReportRepository;

    @Mock
    private MidReportCommandService midReportCommandService;

    @Mock
    private TeamThreadCommandService teamThreadCommandService;

    @Mock
    private TeamThreadQueryService teamThreadQueryService;

    @Mock
    private TeamMessageCommandService teamMessageCommandService;

    @Mock
    private TeamMessageQueryService teamMessageQueryService;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private FileObjectRepository fileObjectRepository;

    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private MidReportAdminFacade midReportAdminFacade;

    @Test
    @DisplayName("담당 교수는 특정 팀의 중간보고서 상세를 이미지 presigned URL 및 제출자 정보와 함께 조회할 수 있다")
    void getMidReport_Success() {
        Team team = Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("A팀").build();
        Milestone milestone = midReportMilestone();
        MidReport report = createSampleReport();

        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(milestone));
        given(projectRepository.findAllByTeamId(TEAM_ID)).willReturn(List.of());
        given(midReportCommandService.getOrCreate(eq(TEAM_ID), eq(MILESTONE_ID), anyString(), any(), anyString(), anyString()))
            .willReturn(report);

        TeamMember leader = TeamMember.builder().teamId(TEAM_ID).userId(STUDENT_ID).isLeader(true).build();
        given(teamMemberRepository.findLeaderByTeamId(TEAM_ID)).willReturn(Optional.of(leader));
        given(teamMemberRepository.findAllByTeamId(TEAM_ID)).willReturn(List.of(leader));

        User student = User.create(STUDENT_ID, "student@kyonggi.ac.kr", "홍길동", "pw", UserGlobalRole.USER, "010-0000-0000");
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(anyList())).willReturn(List.of(student));

        FileObject imageFile = FileObject.create(STUDENT_ID, "images/screen1.png", "화면1.png", "image/png", 1024L, false, null);
        given(fileObjectRepository.findById(1L)).willReturn(Optional.of(imageFile));
        given(fileStorage.presignedUrl("images/screen1.png")).willReturn("https://s3.amazonaws.com/presigned-url");

        MidReportAdminResponse response = midReportAdminFacade.getMidReport(SECTION_ID, TEAM_ID, PROFESSOR_ID);

        assertThat(response.id()).isEqualTo(REPORT_ID);
        assertThat(response.teamName()).isEqualTo("A팀");
        assertThat(response.leaderName()).isEqualTo("홍길동");
        assertThat(response.submittedByName()).isEqualTo("홍길동");
        assertThat(response.status()).isEqualTo(MidReportStatus.SUBMITTED);
        assertThat(response.blocks()).hasSize(4);

        var guiBlock = response.blocks().stream()
            .filter(b -> b.key().equals("gui-design"))
            .findFirst()
            .orElseThrow();
        assertThat(guiBlock.fields().toString()).contains("https://s3.amazonaws.com/presigned-url");
    }

    @Test
    @DisplayName("담당 교수가 아니면 중간보고서를 조회할 수 없다")
    void getMidReport_RejectsForeignProfessor() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(false);

        assertThatThrownBy(() -> midReportAdminFacade.getMidReport(SECTION_ID, TEAM_ID, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 팀만 접근할 수 있습니다.");
    }

    @Test
    @DisplayName("분반에 속하지 않은 팀의 중간보고서는 조회할 수 없다")
    void getMidReport_RejectsForeignTeam() {
        Team otherSectionTeam = Team.builder().id(TEAM_ID).sectionId(999L).name("타분반팀").build();

        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(otherSectionTeam));

        assertThatThrownBy(() -> midReportAdminFacade.getMidReport(SECTION_ID, TEAM_ID, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("해당 분반에 속한 팀이 아닙니다.");
    }

    @Test
    @DisplayName("중간보고서 피드백을 등록하면 TeamMessage로 발행되고 보고서가 수정 요청(REVISION_REQUESTED) 상태로 리오픈된다")
    void postFeedback_Success() {
        Team team = Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("A팀").build();
        Milestone milestone = midReportMilestone();
        MidReport report = createSampleReport();
        TeamThread thread = TeamThread.builder().id(50L).teamId(TEAM_ID).build();
        TeamMessage message = TeamMessage.builder()
            .id(500L)
            .threadId(50L)
            .senderId(PROFESSOR_ID)
            .relatedType(TeamMessageRelatedType.MID_REPORT)
            .relatedId(REPORT_ID)
            .message("GUI 화면 흐름을 보완해주세요.")
            .createdAt(LocalDateTime.of(2026, 9, 13, 14, 0))
            .build();
        User professor = User.create(PROFESSOR_ID, "prof@kyonggi.ac.kr", "김교수", "pw", UserGlobalRole.ADMIN, "010-1111-2222");

        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(milestone));
        given(midReportRepository.findByTeamIdAndMilestoneId(TEAM_ID, MILESTONE_ID)).willReturn(Optional.of(report));
        given(teamThreadCommandService.getOrCreateThread(TEAM_ID)).willReturn(thread);
        given(teamMessageCommandService.postMessage(
            eq(50L), eq(PROFESSOR_ID), eq(TeamMessageRelatedType.MID_REPORT), eq(REPORT_ID), eq("GUI 화면 흐름을 보완해주세요.")))
            .willReturn(message);
        given(userQueryService.getUserByStudentNumber(PROFESSOR_ID)).willReturn(professor);

        MidReportFeedbackAdminRequest request = new MidReportFeedbackAdminRequest(
            "GUI 화면 흐름을 보완해주세요.",
            List.of("gui-design")
        );

        MidReportFeedbackAdminResponse response = midReportAdminFacade.postFeedback(
            SECTION_ID, TEAM_ID, request, PROFESSOR_ID);

        assertThat(response.messageId()).isEqualTo(500L);
        assertThat(response.teamId()).isEqualTo(TEAM_ID);
        assertThat(response.midReportId()).isEqualTo(REPORT_ID);
        assertThat(response.senderName()).isEqualTo("김교수");
        assertThat(response.message()).isEqualTo("GUI 화면 흐름을 보완해주세요.");

        verify(midReportCommandService).requestRevision(eq(REPORT_ID), eq(List.of("gui-design")), any());
        verify(teamMessageCommandService).postMessage(
            eq(50L), eq(PROFESSOR_ID), eq(TeamMessageRelatedType.MID_REPORT), eq(REPORT_ID), eq("GUI 화면 흐름을 보완해주세요."));
    }

    @Test
    @DisplayName("중간보고서가 존재하지 않으면 피드백을 등록할 수 없다")
    void postFeedback_ReportNotFound_ThrowsException() {
        Team team = Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("A팀").build();
        Milestone milestone = midReportMilestone();

        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(milestone));
        given(midReportRepository.findByTeamIdAndMilestoneId(TEAM_ID, MILESTONE_ID)).willReturn(Optional.empty());

        MidReportFeedbackAdminRequest request = new MidReportFeedbackAdminRequest(
            "피드백입니다.",
            List.of()
        );

        assertThatThrownBy(() -> midReportAdminFacade.postFeedback(SECTION_ID, TEAM_ID, request, PROFESSOR_ID))
            .isInstanceOf(MidReportNotFoundException.class);
    }

    @Test
    @DisplayName("중간보고서 피드백 이력을 최신순 페이징으로 조회한다")
    void getFeedbacks_Success() {
        Team team = Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("A팀").build();
        Milestone milestone = midReportMilestone();
        MidReport report = createSampleReport();
        TeamThread thread = TeamThread.builder().id(50L).teamId(TEAM_ID).build();
        TeamMessage message = TeamMessage.builder()
            .id(500L)
            .threadId(50L)
            .senderId(PROFESSOR_ID)
            .relatedType(TeamMessageRelatedType.MID_REPORT)
            .relatedId(REPORT_ID)
            .message("수정 요청 피드백")
            .createdAt(LocalDateTime.of(2026, 9, 13, 14, 0))
            .build();
        Pageable pageable = PageRequest.of(0, 20);
        User professor = User.create(PROFESSOR_ID, "prof@kyonggi.ac.kr", "김교수", "pw", UserGlobalRole.ADMIN, "010-1111-2222");

        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(milestone));
        given(midReportRepository.findByTeamIdAndMilestoneId(TEAM_ID, MILESTONE_ID)).willReturn(Optional.of(report));
        given(teamThreadQueryService.getThread(TEAM_ID)).willReturn(thread);
        given(teamMessageQueryService.getMessages(50L, TeamMessageRelatedType.MID_REPORT, pageable))
            .willReturn(new PageImpl<>(List.of(message), pageable, 1));
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(List.of(PROFESSOR_ID)))
            .willReturn(List.of(professor));

        MidReportFeedbackAdminPageResponse response = midReportAdminFacade.getFeedbacks(
            SECTION_ID, TEAM_ID, pageable, PROFESSOR_ID);

        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().get(0).messageId()).isEqualTo(500L);
        assertThat(response.contents().get(0).senderName()).isEqualTo("김교수");
        assertThat(response.contents().get(0).message()).isEqualTo("수정 요청 피드백");
    }

    @Test
    @DisplayName("팀 스레드가 아직 없으면 빈 피드백 목록을 반환한다")
    void getFeedbacks_NoThread_ReturnsEmptyPage() {
        Team team = Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("A팀").build();
        Milestone milestone = midReportMilestone();

        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(milestone));
        given(midReportRepository.findByTeamIdAndMilestoneId(TEAM_ID, MILESTONE_ID)).willReturn(Optional.empty());
        given(teamThreadQueryService.getThread(TEAM_ID)).willReturn(null);

        Pageable pageable = PageRequest.of(0, 20);
        MidReportFeedbackAdminPageResponse response = midReportAdminFacade.getFeedbacks(
            SECTION_ID, TEAM_ID, pageable, PROFESSOR_ID);

        assertThat(response.contents()).isEmpty();
        assertThat(response.pageable().totalElements()).isZero();
    }

    private Milestone midReportMilestone() {
        return Milestone.restore(
            MILESTONE_ID, SECTION_ID, "중간보고서", null, 8, MilestoneStatus.PUBLISHED,
            new MilestoneSchedule(null, LocalDateTime.of(2026, 9, 30, 23, 59), null, null, null, null),
            MilestoneType.MID_REPORT);
    }

    private MidReport createSampleReport() {
        try {
            var topicFields = OBJECT_MAPPER.readTree("""
                [{"key":"title","value":"스마트 캠퍼스"},{"key":"description","value":"주제 설명"}]
                """);
            var guiFields = OBJECT_MAPPER.readTree("""
                [{"key":"guiScreens","value":"[{\\"screenName\\":\\"메인화면\\",\\"imageFileId\\":1}]"}]
                """);
            var engineFields = OBJECT_MAPPER.readTree("""
                [{"key":"features","value":"기능 목록"},{"key":"architecture","value":"아키텍처"},{"key":"testCases","value":"테스트"}]
                """);
            var planFields = OBJECT_MAPPER.readTree("""
                [{"key":"completed","value":"완료"},{"key":"inProgress","value":"진행"},{"key":"remaining","value":"미구현"},{"key":"help","value":"지원"}]
                """);

            MidReportBlock block1 = MidReportBlock.builder()
                .id(1L)
                .key(MidReportBlockDefinition.TOPIC.key())
                .fields(topicFields)
                .status(MidReportBlockStatus.COMPLETED)
                .lastEditedBy(STUDENT_ID)
                .lastSavedAt(LocalDateTime.of(2026, 9, 10, 10, 0))
                .build();
            MidReportBlock block2 = MidReportBlock.builder()
                .id(2L)
                .key(MidReportBlockDefinition.GUI_DESIGN.key())
                .fields(guiFields)
                .status(MidReportBlockStatus.COMPLETED)
                .lastEditedBy(STUDENT_ID)
                .lastSavedAt(LocalDateTime.of(2026, 9, 10, 10, 0))
                .build();
            MidReportBlock block3 = MidReportBlock.builder()
                .id(3L)
                .key(MidReportBlockDefinition.ENGINE_DESIGN.key())
                .fields(engineFields)
                .status(MidReportBlockStatus.COMPLETED)
                .lastEditedBy(STUDENT_ID)
                .lastSavedAt(LocalDateTime.of(2026, 9, 10, 10, 0))
                .build();
            MidReportBlock block4 = MidReportBlock.builder()
                .id(4L)
                .key(MidReportBlockDefinition.PROJECT_PLAN.key())
                .fields(planFields)
                .status(MidReportBlockStatus.COMPLETED)
                .lastEditedBy(STUDENT_ID)
                .lastSavedAt(LocalDateTime.of(2026, 9, 10, 10, 0))
                .build();

            MidReportRevision revision = new MidReportRevision(
                List.of("gui-design"),
                List.of(),
                LocalDateTime.of(2026, 9, 11, 15, 0),
                null
            );

            return MidReport.builder()
                .id(REPORT_ID)
                .teamId(TEAM_ID)
                .milestoneId(MILESTONE_ID)
                .title("A팀 중간보고서")
                .version(1L)
                .status(MidReportStatus.SUBMITTED)
                .dueDate(LocalDateTime.of(2026, 9, 30, 23, 59))
                .submittedAt(LocalDateTime.of(2026, 9, 10, 12, 0))
                .submittedBy(STUDENT_ID)
                .revision(revision)
                .blocks(List.of(block1, block2, block3, block4))
                .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
