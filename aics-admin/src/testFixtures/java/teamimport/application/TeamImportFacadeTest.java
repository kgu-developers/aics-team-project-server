package teamimport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import kgu.developers.admin.importcommon.RowStatus;
import kgu.developers.admin.teamimport.application.TeamImportFacade;
import kgu.developers.admin.teamimport.application.TeamImportRow;
import kgu.developers.admin.teamimport.application.TeamImportSummary;
import kgu.developers.admin.teamimport.presentation.response.TeamImportApplyResponse;
import kgu.developers.admin.teamimport.presentation.response.TeamImportPreviewResponse;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.domain.importBatch.domain.ImportBatchRepository;
import kgu.developers.domain.importBatch.domain.Type;
import kgu.developers.domain.importBatch.exception.ImportBatchHasInvalidRowsException;
import kgu.developers.admin.importcommon.SectionStaffValidator;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.domain.UserRepository;

public class TeamImportFacadeTest {

  private static final Long SECTION_ID = 1L;
  private static final String ASSISTANT = "202400001";
  private static final String STUDENT_A = "202400002";
  private static final String STUDENT_B = "202400003";
  private static final String STUDENT_C = "202400004";
  private static final String OUTSIDER = "202499999";

  private ImportBatchRepository importBatchRepository;
  private EnrollmentRepository enrollmentRepository;
  private TeamRepository teamRepository;
  private TeamMemberRepository teamMemberRepository;
  private SectionRepository sectionRepository;
  private UserRepository userRepository;
  private TeamImportFacade facade;

  @BeforeEach
  public void init() {
    importBatchRepository = mock(ImportBatchRepository.class);
    enrollmentRepository = mock(EnrollmentRepository.class);
    teamRepository = mock(TeamRepository.class);
    teamMemberRepository = mock(TeamMemberRepository.class);
    sectionRepository = mock(SectionRepository.class);
    userRepository = mock(UserRepository.class);
    facade = new TeamImportFacade(importBatchRepository, enrollmentRepository, teamRepository,
        teamMemberRepository, sectionRepository,
        new SectionStaffValidator(enrollmentRepository, sectionRepository, userRepository));

    given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(mock(SectionDetail.class)));
    given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, ASSISTANT))
        .willReturn(Optional.of(Enrollment.create(SECTION_ID, ASSISTANT, Role.ASSISTANT, Status.ACTIVE)));
    given(enrollmentRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(
        Enrollment.create(SECTION_ID, ASSISTANT, Role.ASSISTANT, Status.ACTIVE),
        Enrollment.create(SECTION_ID, STUDENT_A, Role.STUDENT, Status.ACTIVE),
        Enrollment.create(SECTION_ID, STUDENT_B, Role.STUDENT, Status.ACTIVE),
        Enrollment.create(SECTION_ID, STUDENT_C, Role.STUDENT, Status.ACTIVE)));
    given(teamRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of());
    given(importBatchRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 1L));
  }

  @Test
  @DisplayName("preview는 수강 등록·중복·팀장 중복을 검증한다")
  public void preview_ClassifiesRows() throws IOException {
    // given
    MockMultipartFile file = excel(
        new String[] { "1팀", STUDENT_A, "홍길동", "Y", "백엔드" },
        new String[] { "1팀", STUDENT_B, "김철수", "", "프론트" },
        new String[] { "1팀", STUDENT_C, "박민수", "Y", "" },
        new String[] { "2팀", OUTSIDER, "이영희", "", "" },
        new String[] { "2팀", STUDENT_A, "홍길동", "", "" },
        new String[] { "", STUDENT_C, "팀없음", "", "" });

    // when
    TeamImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT, file);

    // then
    assertThat(response.importId()).isEqualTo(1L);
    assertThat(response.summary()).isEqualTo(new TeamImportSummary(6, 2, 2, 0, 4));

    List<TeamImportRow> rows = response.rows();
    assertThat(rows.get(0).status()).isEqualTo(RowStatus.VALID);
    assertThat(rows.get(0).leader()).isTrue();
    assertThat(rows.get(0).projectRole()).isEqualTo("백엔드");
    assertThat(rows.get(1).status()).isEqualTo(RowStatus.VALID);
    assertThat(rows.get(2).status()).isEqualTo(RowStatus.INVALID); // 1팀 팀장 중복
    assertThat(rows.get(3).status()).isEqualTo(RowStatus.INVALID); // 수강 등록 안 됨
    assertThat(rows.get(4).status()).isEqualTo(RowStatus.INVALID); // 파일 내 중복 학번
    assertThat(rows.get(5).status()).isEqualTo(RowStatus.INVALID); // 팀명 없음
  }

  @Test
  @DisplayName("preview는 이미 편성된 학생을 같은 팀이면 건너뛰고 다른 팀이면 오류로 본다")
  public void preview_ChecksExistingTeams() throws IOException {
    // given
    Team team = Team.builder().id(10L).sectionId(SECTION_ID).name("1팀").build();
    given(teamRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(team));
    given(teamMemberRepository.findAllByTeamId(10L))
        .willReturn(List.of(TeamMember.create(10L, STUDENT_A, false, null)));

    // when
    TeamImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT, excel(
        new String[] { "1팀", STUDENT_A, "홍길동", "", "" },
        new String[] { "2팀", STUDENT_B, "김철수", "", "" }));

    // then
    assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.DUPLICATE);

    // when: 같은 학생을 다른 팀에 넣으면 오류
    TeamImportPreviewResponse moved = facade.preview(SECTION_ID, ASSISTANT,
        excel(new String[] { "3팀", STUDENT_A, "홍길동", "", "" }));

    // then
    assertThat(moved.rows().get(0).status()).isEqualTo(RowStatus.INVALID);
    assertThat(moved.rows().get(0).message()).contains("1팀");
  }

  @Test
  @DisplayName("preview는 그 분반 조교도 담당 교수도 관리자도 아니면 거부한다")
  public void preview_RejectsNonStaff() throws IOException {
    // when & then
    assertThatThrownBy(() -> facade.preview(SECTION_ID, STUDENT_A,
        excel(new String[] { "1팀", STUDENT_A, "홍길동", "", "" })))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("preview는 없는 분반이면 거부한다")
  public void preview_RejectsUnknownSection() throws IOException {
    // given
    given(sectionRepository.findById(999L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> facade.preview(999L, ASSISTANT,
        excel(new String[] { "1팀", STUDENT_A, "홍길동", "", "" })))
        .isInstanceOf(SectionNotFoundException.class);
  }

  @Test
  @DisplayName("apply는 없는 팀을 만들고 팀원을 편성한다")
  public void apply_CreatesTeamsAndMembers() {
    // given
    ImportBatch batch = batch(0, List.of(
        row(2, "1팀", STUDENT_A, true, "백엔드", RowStatus.VALID),
        row(3, "1팀", STUDENT_B, false, "", RowStatus.VALID),
        row(4, "2팀", STUDENT_C, false, "", RowStatus.VALID)));
    given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));
    given(teamRepository.save(any())).willAnswer(invocation -> {
      Team team = invocation.getArgument(0);
      return Team.builder().id(team.getName().equals("1팀") ? 10L : 20L)
          .sectionId(team.getSectionId()).name(team.getName()).status(team.getStatus()).build();
    });

    // when
    TeamImportApplyResponse response = facade.apply(1L, ASSISTANT);

    // then
    assertThat(response.createdTeams()).isEqualTo(2);
    assertThat(response.appliedMembers()).isEqualTo(3);
    assertThat(response.skipped()).isZero();

    ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
    verify(teamMemberRepository, org.mockito.Mockito.times(3)).save(captor.capture());
    assertThat(captor.getAllValues()).extracting(TeamMember::getTeamId, TeamMember::getUserId,
        TeamMember::isLeader, TeamMember::getProjectRole)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(10L, STUDENT_A, true, "백엔드"),
            org.assertj.core.groups.Tuple.tuple(10L, STUDENT_B, false, ""),
            org.assertj.core.groups.Tuple.tuple(20L, STUDENT_C, false, ""));
  }

  @Test
  @DisplayName("apply는 팀에서 빠졌던 학생을 되살려 편성한다")
  public void apply_ReactivatesRemovedMember() {
    // given: (team_id, user_id) 유니크 제약 때문에 새로 넣으면 안 되는 상황
    given(teamRepository.findAllBySectionId(SECTION_ID))
        .willReturn(List.of(Team.builder().id(10L).sectionId(SECTION_ID).name("1팀").build()));
    TeamMember removed = TeamMember.builder().id(5L).teamId(10L).userId(STUDENT_A)
        .isLeader(false).projectRole("").deletedAt(LocalDateTime.now().minusDays(1)).build();
    given(teamMemberRepository.findIncludingDeleted(10L, STUDENT_A)).willReturn(Optional.of(removed));
    ImportBatch batch = batch(0, List.of(row(2, "1팀", STUDENT_A, true, "백엔드", RowStatus.VALID)));
    given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));

    // when
    TeamImportApplyResponse response = facade.apply(1L, ASSISTANT);

    // then
    assertThat(response.appliedMembers()).isEqualTo(1);
    assertThat(response.skipped()).isZero();
    ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
    verify(teamMemberRepository).save(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(5L);
    assertThat(captor.getValue().getDeletedAt()).isNull();
    assertThat(captor.getValue().isLeader()).isTrue();
    assertThat(captor.getValue().getProjectRole()).isEqualTo("백엔드");
  }

  @Test
  @DisplayName("apply가 만드는 팀과 팀원은 NOT NULL 컬럼을 빈 값으로라도 채운다")
  public void apply_FillsNotNullColumns() {
    // given
    ImportBatch batch = batch(0, List.of(row(2, "1팀", STUDENT_A, false, "", RowStatus.VALID)));
    given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));
    given(teamRepository.save(any())).willAnswer(invocation -> Team.builder().id(10L)
        .sectionId(SECTION_ID).name("1팀").build());

    // when
    facade.apply(1L, ASSISTANT);

    // then
    ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
    verify(teamRepository).save(teamCaptor.capture());
    assertThat(teamCaptor.getValue().getKickoffRule()).isNotNull();
    assertThat(teamCaptor.getValue().getMeetingSchedule()).isNotNull();

    ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
    verify(teamMemberRepository).save(memberCaptor.capture());
    assertThat(memberCaptor.getValue().getProjectRole()).isNotNull();
  }

  @Test
  @DisplayName("preview는 컬럼 길이를 넘는 값을 오류로 표시한다")
  public void preview_RejectsTooLongValues() throws IOException {
    // when
    TeamImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT,
        excel(new String[] { "1팀", STUDENT_A, "홍길동", "", "역".repeat(51) }));

    // then
    assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.INVALID);
    assertThat(response.rows().get(0).message()).contains("50자");
  }

  @Test
  @DisplayName("apply는 같은 이름의 기존 팀에 편성한다")
  public void apply_ReusesExistingTeam() {
    // given
    given(teamRepository.findAllBySectionId(SECTION_ID))
        .willReturn(List.of(Team.builder().id(10L).sectionId(SECTION_ID).name("1팀").build()));
    ImportBatch batch = batch(0, List.of(row(2, "1팀", STUDENT_A, false, "", RowStatus.VALID)));
    given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));

    // when
    TeamImportApplyResponse response = facade.apply(1L, ASSISTANT);

    // then
    assertThat(response.createdTeams()).isZero();
    assertThat(response.appliedMembers()).isEqualTo(1);
    verify(teamRepository, never()).save(any());
  }

  @Test
  @DisplayName("apply는 미리보기 이후 수강이 빠진 학생을 건너뛴다")
  public void apply_SkipsUnenrolledStudent() {
    given(enrollmentRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(
        Enrollment.create(SECTION_ID, ASSISTANT, Role.ASSISTANT, Status.ACTIVE),
        Enrollment.create(SECTION_ID, STUDENT_B, Role.STUDENT, Status.ACTIVE)));
    ImportBatch batch = batch(0, List.of(
        row(2, "1팀", STUDENT_A, false, "", RowStatus.VALID),
        row(3, "1팀", STUDENT_B, false, "", RowStatus.VALID)));
    given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));
    given(teamRepository.save(any())).willAnswer(invocation -> Team.builder().id(10L)
        .sectionId(SECTION_ID).name("1팀").build());

    // when
    TeamImportApplyResponse response = facade.apply(1L, ASSISTANT);

    // then
    assertThat(response.appliedMembers()).isEqualTo(1);
    assertThat(response.skipped()).isEqualTo(1);
    ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
    verify(teamMemberRepository).save(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(STUDENT_B);
  }

  @Test
  @DisplayName("apply는 오류 행이 남아 있으면 거부한다")
  public void apply_RejectsInvalidRows() {
    // given
    ImportBatch batch = batch(1, List.of(row(2, "1팀", OUTSIDER, false, "", RowStatus.INVALID)));
    given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));

    // when & then
    assertThatThrownBy(() -> facade.apply(1L, ASSISTANT))
        .isInstanceOf(ImportBatchHasInvalidRowsException.class);
    verify(teamRepository, never()).save(any());
    verify(teamMemberRepository, never()).save(any());
  }

  private TeamImportRow row(int rowNumber, String teamName, String studentNumber, boolean leader,
      String projectRole, RowStatus status) {
    return new TeamImportRow(rowNumber, teamName, studentNumber, "이름", leader, projectRole, status, null);
  }

  private ImportBatch batch(int invalid, List<TeamImportRow> rows) {
    return ImportBatch.builder()
        .id(1L)
        .uploadedBy(ASSISTANT)
        .sectionId(SECTION_ID)
        .type(Type.TEAM)
        .status(kgu.developers.domain.importBatch.domain.Status.PREVIEW)
        .payload(JsonConverter.toTree(rows))
        .summary(JsonConverter.toTree(new TeamImportSummary(rows.size(), 1, 0, 0, invalid)))
        .expiredAt(LocalDateTime.now().plusMinutes(30))
        .build();
  }

  private ImportBatch withId(ImportBatch batch, Long id) {
    return ImportBatch.builder()
        .id(id)
        .uploadedBy(batch.getUploadedBy())
        .sectionId(batch.getSectionId())
        .type(batch.getType())
        .status(batch.getStatus())
        .payload(batch.getPayload())
        .summary(batch.getSummary())
        .expiredAt(batch.getExpiredAt())
        .build();
  }

  private MockMultipartFile excel(String[]... rows) throws IOException {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet();
      String[][] all = new String[rows.length + 1][];
      all[0] = new String[] { "팀명", "학번", "성명", "팀장", "역할" };
      System.arraycopy(rows, 0, all, 1, rows.length);
      for (int i = 0; i < all.length; i++) {
        Row row = sheet.createRow(i);
        for (int j = 0; j < all[i].length; j++) {
          row.createCell(j).setCellValue(all[i][j]);
        }
      }
      workbook.write(out);
      return new MockMultipartFile("file", "teams.xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
    }
  }
}
