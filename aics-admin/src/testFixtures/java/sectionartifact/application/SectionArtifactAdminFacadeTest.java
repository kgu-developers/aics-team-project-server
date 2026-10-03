package sectionartifact.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import kgu.developers.admin.sectionartifact.application.SectionArtifactAdminFacade;
import kgu.developers.admin.sectionartifact.presentation.response.SectionArtifactMemberAdminResponse;
import kgu.developers.admin.sectionartifact.presentation.response.SectionArtifactSummaryAdminListResponse;
import kgu.developers.admin.sectionartifact.presentation.response.SectionArtifactSummaryAdminResponse;
import kgu.developers.common.response.FileDownload;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.submission.domain.ArtifactType;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.submission.application.command.SectionArtifactExcelCommandService;
import kgu.developers.domain.submission.application.query.SectionArtifactQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;

import mock.repository.FakeFileObjectRepository;
import mock.repository.FakeMeetingRecordRepository;
import mock.repository.FakeSubmissionArtifactRepository;
import mock.repository.FakeSubmissionRepository;
import mock.repository.FakeSubmissionVersionRepository;
import mock.repository.FakeTeamMemberRepository;
import mock.repository.FakeTeamRepository;
import mock.repository.FakeUserRepository;

class SectionArtifactAdminFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final Long PROPOSAL_MILESTONE_ID = 10L;
    private static final Long FINAL_MILESTONE_ID = 11L;
    private static final Long PEER_MILESTONE_ID = 12L;
    private static final String PROFESSOR = "professor1";
    private static final String OTHER_PROFESSOR = "professor2";
    private static final LocalDate AS_OF = LocalDate.of(2026, 11, 20);
    // 기준일을 생략했을 때 "오늘"이 KST 기준으로 잡히는지 보려면 시계를 고정해야 한다.
    private static final Clock SERVICE_CLOCK = Clock.fixed(
            LocalDate.of(2026, 11, 20).atTime(23, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime PROPOSAL_DUE_AT = LocalDateTime.of(2026, 11, 15, 23, 59);

    private MilestoneRepository milestoneRepository;
    private SectionQueryService sectionQueryService;
    private FakeTeamRepository teamRepository;
    private FakeTeamMemberRepository teamMemberRepository;
    private FakeSubmissionRepository submissionRepository;
    private FakeSubmissionVersionRepository submissionVersionRepository;
    private FakeSubmissionArtifactRepository submissionArtifactRepository;
    private FakeFileObjectRepository fileObjectRepository;
    private FakeMeetingRecordRepository meetingRecordRepository;
    private SectionArtifactAdminFacade facade;
    private MidReportRepository midReportRepository;
    private Long teamId;

    @BeforeEach
    void setUp() {
        milestoneRepository = mock(MilestoneRepository.class);
        sectionQueryService = mock(SectionQueryService.class);
        teamRepository = new FakeTeamRepository();
        teamMemberRepository = new FakeTeamMemberRepository();
        submissionRepository = new FakeSubmissionRepository();
        submissionVersionRepository = new FakeSubmissionVersionRepository();
        submissionArtifactRepository = new FakeSubmissionArtifactRepository();
        fileObjectRepository = new FakeFileObjectRepository();
        meetingRecordRepository = new FakeMeetingRecordRepository();
        midReportRepository = mock(MidReportRepository.class);

        FakeUserRepository userRepository = new FakeUserRepository();
        userRepository.save(User.create(
                "20261234", "leader@kyonggi.ac.kr", "김철수", "pw", UserGlobalRole.USER, "010-0000-0001"));
        userRepository.save(User.create(
                "20261235", "member@kyonggi.ac.kr", "이영희", "pw", UserGlobalRole.USER, "010-0000-0002"));

        SectionArtifactQueryService sectionArtifactQueryService = new SectionArtifactQueryService(
                milestoneRepository,
                teamRepository,
                teamMemberRepository,
                submissionRepository,
                submissionVersionRepository,
                submissionArtifactRepository,
                fileObjectRepository,
                meetingRecordRepository,
                midReportRepository,
                userRepository);
        facade = new SectionArtifactAdminFacade(SERVICE_CLOCK,
                sectionQueryService, sectionArtifactQueryService, new SectionArtifactExcelCommandService());

        teamId = teamRepository.save(Team.builder()
                .sectionId(SECTION_ID)
                .name("1팀")
                .status(Status.CONFIRMED)
                .build()).getId();
        teamMemberRepository.save(TeamMember.create(teamId, "20261235", false, null));
        teamMemberRepository.save(TeamMember.create(teamId, "20261234", true, null));

        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(sectionQueryService.getSectionById(SECTION_ID)).willReturn(new SectionDetail(
                Section.builder().id(SECTION_ID).code("OOP-01").classTime("월3").build(), null, null));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(
                milestone(PROPOSAL_MILESTONE_ID, MilestoneType.PROPOSAL, PROPOSAL_DUE_AT, MilestoneStatus.PUBLISHED),
                milestone(FINAL_MILESTONE_ID, MilestoneType.FINAL_REPORT,
                        LocalDateTime.of(2026, 11, 18, 23, 59), MilestoneStatus.PUBLISHED),
                // 제출 대상이 아닌 단계와 임시 저장 단계는 집계에서 빠져야 한다.
                milestone(PEER_MILESTONE_ID, MilestoneType.PEER_EVALUATION,
                        LocalDateTime.of(2026, 11, 10, 23, 59), MilestoneStatus.PUBLISHED),
                milestone(13L, MilestoneType.PRESENTATION,
                        LocalDateTime.of(2026, 11, 10, 23, 59), MilestoneStatus.DRAFT)));
    }

    @Test
    @DisplayName("담당하지 않는 분반의 산출물은 다운로드할 수 없다")
    void downloadArtifactsExcel_RejectsOtherSectionStaff() {
        assertThatThrownBy(() -> facade.downloadArtifactsExcel(SECTION_ID, AS_OF, OTHER_PROFESSOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("기준일까지의 제출 이력으로 팀별 요약과 단계별 제출 현황을 집계한다")
    void downloadArtifactsExcel_AggregatesUntilAsOf() throws Exception {
        givenProposalSubmittedLate();

        FileDownload download = facade.downloadArtifactsExcel(SECTION_ID, AS_OF, PROFESSOR);

        assertThat(download.fileName()).isEqualTo("월3_OOP-01-산출물-2026-11-20.xlsx");
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download.content()))) {
            Row summary = workbook.getSheet("팀별 요약").getRow(1);
            assertThat(summary.getCell(0).getStringCellValue()).isEqualTo("OOP-01");
            assertThat(summary.getCell(1).getStringCellValue()).isEqualTo("1팀");
            assertThat(summary.getCell(2).getStringCellValue()).isEqualTo("20261234 김철수, 20261235 이영희");
            assertThat(summary.getCell(3).getNumericCellValue()).isEqualTo(1);  // 기준일 이후 회의록 제외
            assertThat(summary.getCell(4).getNumericCellValue()).isEqualTo(3);  // 수정 로그 수(version 합)
            assertThat(summary.getCell(5).getNumericCellValue()).isEqualTo(1);  // 제출 이력 단계 수
            assertThat(summary.getCell(6).getNumericCellValue()).isEqualTo(1);  // 마감된 미제출(최종 보고서)

            Sheet stages = workbook.getSheet("단계별 제출 현황");
            assertThat(stages.getLastRowNum()).isEqualTo(2);  // 제안서·최종 보고서만

            Row proposal = stages.getRow(1);
            assertThat(proposal.getCell(3).getStringCellValue()).isEqualTo("제안서");
            assertThat(proposal.getCell(4).getStringCellValue()).isEqualTo("2026-11-15 23:59");
            assertThat(proposal.getCell(5).getStringCellValue()).isEqualTo("SUBMITTED");
            assertThat(proposal.getCell(6).getStringCellValue()).isEqualTo("2026-11-16 09:10");
            assertThat(proposal.getCell(7).getStringCellValue()).isEqualTo("2026-11-19 20:00");
            assertThat(proposal.getCell(8).getNumericCellValue()).isEqualTo(2);
            assertThat(proposal.getCell(9).getStringCellValue()).isEqualTo("예");
            assertThat(proposal.getCell(10).getNumericCellValue()).isEqualTo(2);
            assertThat(proposal.getCell(11).getNumericCellValue()).isEqualTo(1);
            assertThat(proposal.getCell(12).getNumericCellValue()).isEqualTo(3_000);
            // 용량은 byte 숫자가 커서 천 단위 구분 표시가 있어야 읽을 수 있다
            assertThat(proposal.getCell(12).getCellStyle().getDataFormatString()).isEqualTo("#,##0");

            Row finalReport = stages.getRow(2);
            assertThat(finalReport.getCell(3).getStringCellValue()).isEqualTo("최종 보고서");
            assertThat(finalReport.getCell(5).getStringCellValue()).isEqualTo("NOT_SUBMITTED");
            assertThat(finalReport.getCell(6).getStringCellValue()).isEmpty();
            assertThat(finalReport.getCell(8).getStringCellValue()).isEmpty();  // 셀 자체가 비어 있으면 안 된다
            assertThat(finalReport.getCell(9).getStringCellValue()).isEmpty();
            assertThat(finalReport.getCell(12).getNumericCellValue()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("팀별 요약 조회는 엑셀 첫 시트와 같은 집계를 돌려준다")
    void getArtifactSummary_AggregatesUntilAsOf() {
        givenProposalSubmittedLate();

        SectionArtifactSummaryAdminListResponse response = facade.getArtifactSummary(SECTION_ID, AS_OF, PROFESSOR);

        assertThat(response.sectionId()).isEqualTo(SECTION_ID);
        assertThat(response.sectionName()).isEqualTo("OOP-01");
        assertThat(response.asOf()).isEqualTo("2026-11-20");
        assertThat(response.contents()).hasSize(1);

        SectionArtifactSummaryAdminResponse summary = response.contents().get(0);
        assertThat(summary.teamId()).isEqualTo(teamId);
        assertThat(summary.teamName()).isEqualTo("1팀");
        assertThat(summary.members()).extracting(
                        SectionArtifactMemberAdminResponse::studentNumber, SectionArtifactMemberAdminResponse::name)
                .containsExactly(tuple("20261234", "김철수"), tuple("20261235", "이영희"));
        assertThat(summary.meetingRecordCount()).isEqualTo(1);  // 기준일 이후 회의록 제외
        assertThat(summary.meetingRecordEditCount()).isEqualTo(3);  // 수정 로그 수(version 합)
        assertThat(summary.submittedStageCount()).isEqualTo(1);
        assertThat(summary.overdueMissingStageCount()).isEqualTo(1);  // 최종 보고서
    }

    @Test
    @DisplayName("기준일을 생략하면 KST 기준 오늘까지로 집계한다")
    void getArtifactSummary_DefaultsToTodayInServiceZone() {
        givenProposalSubmittedLate();

        // 고정 시계는 KST 2026-11-20 23:00 — UTC로는 아직 11-20 14:00이라 기본 시간대를 쓰면 날짜가 어긋난다
        SectionArtifactSummaryAdminListResponse response = facade.getArtifactSummary(SECTION_ID, null, PROFESSOR);

        assertThat(response.asOf()).isEqualTo("2026-11-20");
        assertThat(response.contents().get(0).submittedStageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("담당하지 않는 분반의 팀별 요약은 조회할 수 없다")
    void getArtifactSummary_RejectsOtherSectionStaff() {
        assertThatThrownBy(() -> facade.getArtifactSummary(SECTION_ID, AS_OF, OTHER_PROFESSOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    // 제안서는 정시 마감 이후에 최초 제출(지각), 기준일 다음 날 3버전까지 쌓였고, 최종 보고서는 마감이
    // 지났는데 미제출인 상태. 회의록도 기준일 전후로 한 건씩 둔다.
    private void givenProposalSubmittedLate() {
        Submission submission = submissionRepository.save(Submission.builder()
                .teamId(teamId)
                .milestoneId(PROPOSAL_MILESTONE_ID)
                .status(SubmissionStatus.SUBMITTED)
                .currentVersion(3)
                .updatedAt(LocalDateTime.of(2026, 11, 19, 20, 0))
                .build());
        version(submission.getId(), 1, LocalDateTime.of(2026, 11, 16, 9, 10));
        SubmissionVersion latest = version(submission.getId(), 2, LocalDateTime.of(2026, 11, 19, 20, 0));
        SubmissionVersion afterAsOf = version(submission.getId(), 3, LocalDateTime.of(2026, 11, 21, 10, 0));

        submissionArtifactRepository.saveAll(List.of(
                SubmissionArtifact.file(latest.getId(), null, file("report.pdf", "application/pdf", 1_000L)),
                SubmissionArtifact.file(latest.getId(), null, file("shot.png", "image/png", 2_000L)),
                SubmissionArtifact.link(latest.getId(), null, "https://example.com"),
                SubmissionArtifact.file(afterAsOf.getId(), null, file("late.pdf", "application/pdf", 9_000L))));

        meetingRecordRepository.save(meetingRecord(LocalDateTime.of(2026, 11, 10, 10, 0), 3L));
        meetingRecordRepository.save(meetingRecord(LocalDateTime.of(2026, 11, 21, 10, 0), 1L));
    }

    private Milestone milestone(Long id, MilestoneType type, LocalDateTime dueAt, MilestoneStatus status) {
        return Milestone.restore(id, SECTION_ID, type.name(), null, 2, status,
                new MilestoneSchedule(null, dueAt, null, null, null, null), type);
    }

    private SubmissionVersion version(Long submissionId, int version, LocalDateTime submittedAt) {
        return submissionVersionRepository.save(SubmissionVersion.builder()
                .submissionId(submissionId)
                .version(version)
                .submittedBy("20261234")
                .submittedAt(submittedAt)
                .build());
    }

    private Long file(String fileName, String contentType, long size) {
        return fileObjectRepository.save(
                FileObject.create("20261234", "key/" + fileName, fileName, contentType, size, false, null)).getId();
    }

    private MeetingRecord meetingRecord(LocalDateTime createdAt, long version) {
        return MeetingRecord.builder()
                .teamId(teamId)
                .title("회의")
                .phase(MeetingPhase.PROPOSAL)
                .authorId("20261234")
                .meetingAt(createdAt)
                .content("내용")
                .version(version)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
