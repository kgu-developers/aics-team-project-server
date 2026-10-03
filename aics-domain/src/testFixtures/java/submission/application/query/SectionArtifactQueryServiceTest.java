package submission.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.midreport.domain.MidReportRevision;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.submission.application.query.SectionArtifactMember;
import kgu.developers.domain.submission.application.query.SectionArtifactQueryService;
import kgu.developers.domain.submission.application.query.SectionArtifactStageRow;
import kgu.developers.domain.submission.application.query.SectionArtifactTeamRow;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.domain.TeamMember;
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

class SectionArtifactQueryServiceTest {

    private static final Long SECTION_ID = 1L;
    private static final Long PROPOSAL_ID = 10L;
    private static final Long MID_REPORT_ID = 11L;
    private static final Long FINAL_REPORT_ID = 12L;
    private static final Long PRESENTATION_ID = 13L;
    // 기준일: 2026-11-20 끝. 이후에 쌓인 이력은 집계에서 빠져야 한다.
    private static final LocalDateTime UNTIL = LocalDateTime.of(2026, 11, 20, 23, 59, 59, 999_999_999);

    private MilestoneRepository milestoneRepository;
    private MidReportRepository midReportRepository;
    private FakeTeamRepository teamRepository;
    private FakeTeamMemberRepository teamMemberRepository;
    private FakeSubmissionRepository submissionRepository;
    private FakeSubmissionVersionRepository submissionVersionRepository;
    private FakeSubmissionArtifactRepository submissionArtifactRepository;
    private FakeFileObjectRepository fileObjectRepository;
    private FakeMeetingRecordRepository meetingRecordRepository;
    private SectionArtifactQueryService queryService;
    private Long teamId;

    @BeforeEach
    void setUp() {
        milestoneRepository = mock(MilestoneRepository.class);
        midReportRepository = mock(MidReportRepository.class);
        teamRepository = new FakeTeamRepository();
        teamMemberRepository = new FakeTeamMemberRepository();
        submissionRepository = new FakeSubmissionRepository();
        submissionVersionRepository = new FakeSubmissionVersionRepository();
        submissionArtifactRepository = new FakeSubmissionArtifactRepository();
        fileObjectRepository = new FakeFileObjectRepository();
        meetingRecordRepository = new FakeMeetingRecordRepository();

        FakeUserRepository userRepository = new FakeUserRepository();
        userRepository.save(User.create(
                "20261234", "a@kyonggi.ac.kr", "김철수", "pw", UserGlobalRole.USER, "010-0000-0001"));
        userRepository.save(User.create(
                "20261235", "b@kyonggi.ac.kr", "이영희", "pw", UserGlobalRole.USER, "010-0000-0002"));

        queryService = new SectionArtifactQueryService(
                milestoneRepository, teamRepository, teamMemberRepository, submissionRepository,
                submissionVersionRepository, submissionArtifactRepository, fileObjectRepository,
                meetingRecordRepository, midReportRepository, userRepository);

        teamId = teamRepository.save(Team.builder()
                .sectionId(SECTION_ID).name("1팀").status(Status.CONFIRMED).build()).getId();
        teamMemberRepository.save(TeamMember.create(teamId, "20261235", false, "개발"));
        teamMemberRepository.save(TeamMember.create(teamId, "20261234", true, "팀장"));

        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(
                milestone(PROPOSAL_ID, MilestoneType.PROPOSAL, LocalDateTime.of(2026, 11, 15, 23, 59),
                        MilestoneStatus.PUBLISHED),
                milestone(MID_REPORT_ID, MilestoneType.MID_REPORT, LocalDateTime.of(2026, 11, 18, 23, 59),
                        MilestoneStatus.PUBLISHED),
                milestone(FINAL_REPORT_ID, MilestoneType.FINAL_REPORT, LocalDateTime.of(2026, 11, 19, 23, 59),
                        MilestoneStatus.PUBLISHED),
                milestone(PRESENTATION_ID, MilestoneType.PRESENTATION, LocalDateTime.of(2026, 12, 1, 23, 59),
                        MilestoneStatus.PUBLISHED),
                // 제출 대상이 아닌 단계와 임시 저장 단계는 집계 대상에서 빠진다
                milestone(14L, MilestoneType.PEER_EVALUATION, LocalDateTime.of(2026, 11, 10, 23, 59),
                        MilestoneStatus.PUBLISHED),
                milestone(15L, MilestoneType.PRESENTATION, LocalDateTime.of(2026, 11, 10, 23, 59),
                        MilestoneStatus.DRAFT)));
    }

    @Test
    @DisplayName("제출 대상이 아닌 단계와 임시 저장 단계는 집계하지 않는다")
    void getSectionArtifactRows_SkipsNonSubmittableAndDraftMilestones() {
        List<SectionArtifactStageRow> stages = rows().get(0).stages();

        assertThat(stages).extracting(SectionArtifactStageRow::type).containsExactly(
                MilestoneType.PROPOSAL, MilestoneType.MID_REPORT,
                MilestoneType.FINAL_REPORT, MilestoneType.PRESENTATION);
    }

    @Test
    @DisplayName("기준일 이후 제출된 버전은 빼고 최신 버전의 파일만 센다")
    void getSectionArtifactRows_CountsLatestVersionFilesUntilAsOf() {
        Submission submission = submission(PROPOSAL_ID, SubmissionStatus.SUBMITTED, 3);
        version(submission.getId(), 1, LocalDateTime.of(2026, 11, 16, 9, 10));
        SubmissionVersion latest = version(submission.getId(), 2, LocalDateTime.of(2026, 11, 19, 20, 0));
        SubmissionVersion afterAsOf = version(submission.getId(), 3, LocalDateTime.of(2026, 11, 21, 10, 0));

        Long deletedFileId = file("deleted.pdf", "application/pdf", 7_000L, true);
        submissionArtifactRepository.saveAll(List.of(
                SubmissionArtifact.file(latest.getId(), null, file("report.pdf", "application/pdf", 1_000L, false)),
                SubmissionArtifact.file(latest.getId(), null, file("shot.PNG", "IMAGE/PNG", 2_000L, false)),
                SubmissionArtifact.file(latest.getId(), null, deletedFileId),
                SubmissionArtifact.link(latest.getId(), null, "https://example.com"),
                SubmissionArtifact.text(latest.getId(), null, "요약"),
                SubmissionArtifact.file(afterAsOf.getId(), null, file("late.pdf", "application/pdf", 9_000L, false))));

        SectionArtifactStageRow proposal = stage(MilestoneType.PROPOSAL);
        assertThat(proposal.status()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(proposal.firstSubmittedAt()).isEqualTo(LocalDateTime.of(2026, 11, 16, 9, 10));
        assertThat(proposal.lastSubmittedAt()).isEqualTo(LocalDateTime.of(2026, 11, 19, 20, 0));
        assertThat(proposal.latestVersion()).isEqualTo(2);
        // 링크·텍스트는 파일이 아니고, 소프트 삭제된 파일은 개수·용량에서 뺀다. 대소문자 섞인 MIME도 이미지다.
        assertThat(proposal.fileCount()).isEqualTo(2);
        assertThat(proposal.imageCount()).isEqualTo(1);
        assertThat(proposal.totalFileSize()).isEqualTo(3_000L);
        assertThat(proposal.overdueMissing()).isFalse();
    }

    @Test
    @DisplayName("정시 마감 이후 최초 제출이면 지각이고, 마감 안에 냈으면 지각이 아니다")
    void getSectionArtifactRows_MarksFirstSubmissionLate() {
        Submission late = submission(PROPOSAL_ID, SubmissionStatus.SUBMITTED, 1);
        version(late.getId(), 1, LocalDateTime.of(2026, 11, 16, 0, 0));
        Submission onTime = submission(FINAL_REPORT_ID, SubmissionStatus.APPROVED, 1);
        // 마감 정각은 지각이 아니다
        version(onTime.getId(), 1, LocalDateTime.of(2026, 11, 19, 23, 59));

        assertThat(stage(MilestoneType.PROPOSAL).firstSubmissionLate()).isTrue();
        assertThat(stage(MilestoneType.FINAL_REPORT).firstSubmissionLate()).isFalse();
        assertThat(stage(MilestoneType.FINAL_REPORT).status()).isEqualTo(SubmissionStatus.APPROVED);
    }

    @Test
    @DisplayName("제출 이력이 없으면 마감이 지난 단계만 미제출로 세고 지각 여부는 비운다")
    void getSectionArtifactRows_FlagsOverdueMissingOnlyAfterDueAt() {
        SectionArtifactTeamRow row = rows().get(0);

        // 제안서·중간점검·최종 보고서는 기준일 전에 마감, 발표 자료는 기준일 이후 마감
        assertThat(row.overdueMissingStageCount()).isEqualTo(3);
        assertThat(row.submittedStageCount()).isZero();
        assertThat(stage(MilestoneType.PRESENTATION).overdueMissing()).isFalse();
        assertThat(stage(MilestoneType.PROPOSAL).firstSubmissionLate()).isNull();
        assertThat(stage(MilestoneType.PROPOSAL).status()).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
    }

    @Test
    @DisplayName("중간점검은 제출 버전이 없으면 중간보고서에서 상태·제출 시각·버전을 읽는다")
    void getSectionArtifactRows_FallsBackToMidReport() {
        given(midReportRepository.findAllByTeamIdInAndMilestoneId(List.of(teamId), MID_REPORT_ID))
                .willReturn(List.of(MidReport.builder()
                        .id(1L).teamId(teamId).milestoneId(MID_REPORT_ID).version(3L)
                        .status(MidReportStatus.REVISION_REQUESTED)
                        .submittedAt(LocalDateTime.of(2026, 11, 17, 18, 20))
                        .createdAt(LocalDateTime.of(2026, 11, 1, 10, 0))
                        .build()));

        SectionArtifactStageRow midReport = stage(MilestoneType.MID_REPORT);
        assertThat(midReport.status()).isEqualTo(SubmissionStatus.REVISION_REQUESTED);
        assertThat(midReport.firstSubmittedAt()).isEqualTo(LocalDateTime.of(2026, 11, 17, 18, 20));
        assertThat(midReport.latestVersion()).isEqualTo(3);
        assertThat(midReport.firstSubmissionLate()).isFalse();
        // 블록 양식이라 파일 산출물이 없다
        assertThat(midReport.fileCount()).isZero();
        assertThat(midReport.overdueMissing()).isFalse();
    }

    @Test
    @DisplayName("반려 후 재제출한 중간점검은 최초 제출 시각이 남지 않아 지각 여부를 비운다")
    void getSectionArtifactRows_LeavesMidReportLateUnknownAfterRevision() {
        // 마감 11/18 23:59 전에 냈더라도 submit()이 submittedAt을 덮어써서 재제출 시각만 남는다.
        given(midReportRepository.findAllByTeamIdInAndMilestoneId(List.of(teamId), MID_REPORT_ID))
                .willReturn(List.of(MidReport.builder()
                        .id(1L).teamId(teamId).milestoneId(MID_REPORT_ID).version(4L)
                        .status(MidReportStatus.SUBMITTED)
                        .revision(new MidReportRevision(List.of("GOAL"), List.of("GOAL"),
                                LocalDateTime.of(2026, 11, 18, 20, 0), LocalDateTime.of(2026, 11, 19, 10, 0)))
                        .submittedAt(LocalDateTime.of(2026, 11, 19, 10, 0))
                        .createdAt(LocalDateTime.of(2026, 11, 1, 10, 0))
                        .build()));

        SectionArtifactStageRow midReport = stage(MilestoneType.MID_REPORT);
        assertThat(midReport.firstSubmissionLate()).isNull();
        assertThat(midReport.status()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(midReport.firstSubmittedAt()).isEqualTo(LocalDateTime.of(2026, 11, 19, 10, 0));
    }

    @Test
    @DisplayName("기준일 이후에 만들어진 중간보고서는 그때 없던 것으로 본다")
    void getSectionArtifactRows_IgnoresMidReportCreatedAfterAsOf() {
        given(midReportRepository.findAllByTeamIdInAndMilestoneId(List.of(teamId), MID_REPORT_ID))
                .willReturn(List.of(MidReport.builder()
                        .id(1L).teamId(teamId).milestoneId(MID_REPORT_ID).version(1L)
                        .status(MidReportStatus.SUBMITTED)
                        .submittedAt(LocalDateTime.of(2026, 11, 19, 10, 0))
                        .createdAt(LocalDateTime.of(2026, 11, 21, 10, 0))
                        .build()));

        SectionArtifactStageRow midReport = stage(MilestoneType.MID_REPORT);
        assertThat(midReport.status()).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
        assertThat(midReport.firstSubmittedAt()).isNull();
        assertThat(midReport.overdueMissing()).isTrue();
    }

    @Test
    @DisplayName("회의록은 기준일까지 작성된 것만 세고 수정 횟수는 version 합으로 근사한다")
    void getSectionArtifactRows_CountsMeetingRecordsUntilAsOf() {
        meetingRecordRepository.save(meetingRecord(LocalDateTime.of(2026, 11, 10, 10, 0), 2L));
        meetingRecordRepository.save(meetingRecord(LocalDateTime.of(2026, 11, 12, 10, 0), 1L));
        meetingRecordRepository.save(meetingRecord(LocalDateTime.of(2026, 11, 21, 10, 0), 5L));

        SectionArtifactTeamRow row = rows().get(0);
        assertThat(row.meetingRecordCount()).isEqualTo(2);
        assertThat(row.meetingRecordEditCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("팀원은 팀장을 먼저, 나머지는 학번 순으로 담고 이름이 없으면 대체 문구를 쓴다")
    void getSectionArtifactRows_OrdersMembersLeaderFirst() {
        teamMemberRepository.save(TeamMember.create(teamId, "20269999", false, "개발"));

        List<SectionArtifactMember> members = rows().get(0).members();
        assertThat(members).extracting(SectionArtifactMember::studentNumber)
                .containsExactly("20261234", "20261235", "20269999");
        assertThat(members.get(0).displayName()).isEqualTo("김철수");
        // 사용자 정보가 없는 팀원(탈퇴 등)은 학번만 남기지 않고 대체 문구를 쓴다
        assertThat(members.get(2).displayName()).isEqualTo("(탈퇴한 사용자)");
    }

    @Test
    @DisplayName("팀이 없는 분반은 빈 목록을 돌려준다")
    void getSectionArtifactRows_ReturnsEmptyWhenNoTeam() {
        assertThat(queryService.getSectionArtifactRows(SECTION_ID + 1, UNTIL)).isEmpty();
    }

    private List<SectionArtifactTeamRow> rows() {
        return queryService.getSectionArtifactRows(SECTION_ID, UNTIL);
    }

    private SectionArtifactStageRow stage(MilestoneType type) {
        return rows().get(0).stages().stream()
                .filter(stage -> stage.type() == type)
                .findFirst()
                .orElseThrow();
    }

    private Milestone milestone(Long id, MilestoneType type, LocalDateTime dueAt, MilestoneStatus status) {
        return Milestone.restore(id, SECTION_ID, type.name(), null, 2, status,
                new MilestoneSchedule(null, dueAt, null, null, null, null), type);
    }

    private Submission submission(Long milestoneId, SubmissionStatus status, int currentVersion) {
        return submissionRepository.save(Submission.builder()
                .teamId(teamId).milestoneId(milestoneId).status(status).currentVersion(currentVersion).build());
    }

    private SubmissionVersion version(Long submissionId, int version, LocalDateTime submittedAt) {
        return submissionVersionRepository.save(SubmissionVersion.builder()
                .submissionId(submissionId).version(version).submittedBy("20261234").submittedAt(submittedAt).build());
    }

    private Long file(String fileName, String contentType, long size, boolean deleted) {
        FileObject saved = fileObjectRepository.save(FileObject.builder()
                .uploadedBy("20261234").storageKey("key/" + fileName).fileName(fileName)
                .contentType(contentType).size(size)
                .deletedAt(deleted ? LocalDateTime.of(2026, 11, 19, 21, 0) : null)
                .build());
        return saved.getId();
    }

    private MeetingRecord meetingRecord(LocalDateTime createdAt, long version) {
        return MeetingRecord.builder()
                .teamId(teamId).title("회의").phase(MeetingPhase.PROPOSAL).authorId("20261234")
                .meetingAt(createdAt).content("내용").version(version).createdAt(createdAt).build();
    }
}
