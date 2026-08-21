package enrollmentimport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.mock.web.MockMultipartFile;

import kgu.developers.admin.enrollmentimport.application.EnrollmentImportFacade;
import kgu.developers.admin.enrollmentimport.application.EnrollmentImportRow;
import kgu.developers.admin.enrollmentimport.application.EnrollmentImportSummary;
import kgu.developers.admin.importcommon.RowStatus;
import kgu.developers.admin.enrollmentimport.presentation.response.EnrollmentImportApplyResponse;
import kgu.developers.admin.enrollmentimport.presentation.response.EnrollmentImportPreviewResponse;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.enrollment.application.command.EnrollmentCommandService;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.domain.importBatch.domain.ImportBatchRepository;
import kgu.developers.domain.importBatch.domain.Type;
import kgu.developers.domain.importBatch.exception.ImportBatchAlreadyAppliedException;
import kgu.developers.domain.importBatch.exception.ImportBatchHasInvalidRowsException;
import kgu.developers.domain.user.application.command.UserCommandService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;

public class EnrollmentImportFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final String ASSISTANT = "202400001";
    private static final String MEMBER = "202400002";
    private static final String ENROLLED = "202400003";
    private static final String NEWCOMER = "202499999";

    private ImportBatchRepository importBatchRepository;
    private EnrollmentRepository enrollmentRepository;
    private EnrollmentCommandService enrollmentCommandService;
    private UserCommandService userCommandService;
    private UserRepository userRepository;
    private SectionRepository sectionRepository;
    private EnrollmentImportFacade facade;

    @BeforeEach
    public void init() {
        importBatchRepository = mock(ImportBatchRepository.class);
        enrollmentRepository = mock(EnrollmentRepository.class);
        enrollmentCommandService = mock(EnrollmentCommandService.class);
        userCommandService = mock(UserCommandService.class);
        userRepository = mock(UserRepository.class);
        sectionRepository = mock(SectionRepository.class);
        facade = new EnrollmentImportFacade(importBatchRepository, enrollmentRepository,
            enrollmentCommandService, userCommandService, userRepository,
            sectionRepository);

        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(mock(SectionDetail.class)));
        given(enrollmentRepository.findAllBySectionId(SECTION_ID))
            .willReturn(List.of(Enrollment.create(SECTION_ID, ENROLLED, Role.STUDENT, Status.ACTIVE)));
        given(userRepository.findAllByStudentNumberIn(any()))
            .willReturn(List.of(user(MEMBER), user(ENROLLED)));
        given(importBatchRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 1L));
    }

    @Test
    @DisplayName("preview는 행별로 상태를 분류하고 요약을 만든다")
    public void preview_ClassifiesRows() throws IOException {
        // given
        MockMultipartFile file = excel(
            new String[] {MEMBER, "홍길동", "hong@kyonggi.ac.kr", "010-0000-0001", ""},
            new String[] {ENROLLED, "김철수", "kim@kyonggi.ac.kr", "010-0000-0002", "조교"},
            new String[] {NEWCOMER, "이영희", "lee@kyonggi.ac.kr", "010-0000-0003", ""},
            new String[] {MEMBER, "홍길동", "hong@kyonggi.ac.kr", "010-0000-0001", ""},
            new String[] {"", "이름만", "", "", ""},
            new String[] {MEMBER, "역할오류", "", "", "청강"}
        );

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT, file);

        // then
        assertThat(response.importId()).isEqualTo(1L);
        assertThat(response.summary())
            .isEqualTo(new EnrollmentImportSummary(6, 1, 1, 1, 3));

        List<EnrollmentImportRow> rows = response.rows();
        assertThat(rows.get(0).status()).isEqualTo(RowStatus.VALID);
        assertThat(rows.get(0).role()).isEqualTo(Role.STUDENT);
        assertThat(rows.get(0).rowNumber()).isEqualTo(2);
        assertThat(rows.get(1).status()).isEqualTo(RowStatus.DUPLICATE);
        assertThat(rows.get(1).role()).isEqualTo(Role.ASSISTANT);
        assertThat(rows.get(2).status()).isEqualTo(RowStatus.NEW_USER);  // 미가입 → 계정 생성 대상
        assertThat(rows.get(3).status()).isEqualTo(RowStatus.INVALID);   // 파일 내 중복
        assertThat(rows.get(4).status()).isEqualTo(RowStatus.INVALID);   // 학번 없음
        assertThat(rows.get(5).status()).isEqualTo(RowStatus.INVALID);   // 역할 오류
    }

    @Test
    @DisplayName("preview는 이메일이 비면 학번 기준 학교 메일로 채운다")
    public void preview_FillsMissingEmail() throws IOException {
        // given
        MockMultipartFile file = excel(new String[] {NEWCOMER, "이영희", "", "010-0000-0003", ""});

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT, file);

        // then
        assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.NEW_USER);
        assertThat(response.rows().get(0).email()).isEqualTo(NEWCOMER + "@kyonggi.ac.kr");
    }

    @Test
    @DisplayName("preview는 탈퇴 이력이 있는 학번을 오류로 표시한다")
    public void preview_RejectsWithdrawnUser() throws IOException {
        // given
        given(userRepository.findAllIncludingDeletedByStudentNumberIn(any()))
            .willReturn(List.of(user(NEWCOMER)));

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT,
            excel(new String[] {NEWCOMER, "이영희", "lee@kyonggi.ac.kr", "010-0000-0003", ""}));

        // then
        assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.INVALID);
        assertThat(response.rows().get(0).message()).contains("탈퇴");
    }

    @Test
    @DisplayName("preview는 다른 사람이 쓰는 이메일을 오류로 표시한다")
    public void preview_RejectsTakenEmail() throws IOException {
        // given
        given(userRepository.findAllByEmailIn(any())).willReturn(List.of(user(MEMBER)));

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT,
            excel(new String[] {NEWCOMER, "이영희", MEMBER + "@kyonggi.ac.kr", "010-0000-0003", ""}));

        // then
        assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.INVALID);
        assertThat(response.rows().get(0).message()).contains("이미 사용 중인 이메일");
    }

    @Test
    @DisplayName("preview는 수강 취소 상태 학생을 재등록 대상으로 본다")
    public void preview_ReenrollsWithdrawnStudent() throws IOException {
        // given
        given(enrollmentRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(
            Enrollment.create(SECTION_ID, MEMBER, Role.STUDENT, Status.WITHDRAWN)));

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT,
            excel(new String[] {MEMBER, "홍길동", "hong@kyonggi.ac.kr", "010-0000-0001", ""}));

        // then
        assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.VALID);
        assertThat(response.rows().get(0).message()).contains("수강 취소");
    }

    @Test
    @DisplayName("apply는 수강 취소 상태를 다시 활성화한다")
    public void apply_ReactivatesWithdrawnEnrollment() {
        // given
        ImportBatch batch = batch(0, List.of(row(2, MEMBER, RowStatus.VALID)));
        given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, MEMBER)).willReturn(
            Optional.of(Enrollment.create(SECTION_ID, MEMBER, Role.STUDENT, Status.WITHDRAWN)));

        // when
        EnrollmentImportApplyResponse response = facade.apply(1L, ASSISTANT);

        // then
        assertThat(response.applied()).isEqualTo(1);
        assertThat(response.skipped()).isZero();
        verify(enrollmentCommandService).updateEnrollment(SECTION_ID, MEMBER, Role.STUDENT, Status.ACTIVE);
        verify(enrollmentCommandService, never()).createEnrollment(any(), anyString(), any());
    }

    @Test
    @DisplayName("preview는 컬럼 길이를 넘는 값을 오류로 표시한다")
    public void preview_RejectsTooLongValues() throws IOException {
        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT, excel(
            new String[] {MEMBER, "홍길동", "a".repeat(60) + "@kyonggi.ac.kr", "010-0000-0001", ""}));

        // then
        assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.INVALID);
        assertThat(response.rows().get(0).message()).contains("64자");
    }

    @Test
    @DisplayName("preview는 없는 분반이면 거부한다")
    public void preview_RejectsUnknownSection() throws IOException {
        // given
        given(sectionRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> facade.preview(999L, ASSISTANT,
            excel(new String[] {MEMBER, "홍길동", "hong@kyonggi.ac.kr", "010-0000-0001", ""})))
            .isInstanceOf(SectionNotFoundException.class);
    }

    @Test
    @DisplayName("apply는 미가입 학생 계정을 만들고 수강 등록한다")
    public void apply_CreatesUsersAndEnrollments() {
        // given
        ImportBatch batch = batch(0, List.of(
            row(2, MEMBER, RowStatus.VALID),
            row(3, NEWCOMER, RowStatus.NEW_USER),
            row(4, ENROLLED, RowStatus.DUPLICATE)
        ));
        given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));

        // when
        EnrollmentImportApplyResponse response = facade.apply(1L, ASSISTANT);

        // then
        assertThat(response.applied()).isEqualTo(2);
        assertThat(response.createdUsers()).isEqualTo(1);
        assertThat(response.skipped()).isZero();
        verify(userCommandService).createUser(NEWCOMER, NEWCOMER + "@kyonggi.ac.kr", "이름", NEWCOMER,
            UserGlobalRole.USER, "010-0000-0000", false);
        verify(userCommandService, never()).createUser(eq(MEMBER), any(), any(), any(), any(), any(), anyBoolean());
        verify(enrollmentCommandService).createEnrollment(SECTION_ID, MEMBER, Role.STUDENT);
        verify(enrollmentCommandService).createEnrollment(SECTION_ID, NEWCOMER, Role.STUDENT);
        verify(enrollmentCommandService, never()).createEnrollment(eq(SECTION_ID), eq(ENROLLED), any());
    }

    @Test
    @DisplayName("apply는 미리보기 이후 등록된 학생을 건너뛴다")
    public void apply_SkipsAlreadyEnrolled() {
        // given
        ImportBatch batch = batch(0, List.of(row(2, MEMBER, RowStatus.VALID)));
        given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, MEMBER)).willReturn(
            Optional.of(Enrollment.create(SECTION_ID, MEMBER, Role.STUDENT, Status.ACTIVE)));

        // when
        EnrollmentImportApplyResponse response = facade.apply(1L, ASSISTANT);

        // then
        assertThat(response.applied()).isZero();
        assertThat(response.skipped()).isEqualTo(1);
        verify(enrollmentCommandService, never()).createEnrollment(any(), anyString(), any());
    }

    @Test
    @DisplayName("apply는 오류 행이 남아 있으면 거부한다")
    public void apply_RejectsInvalidRows() {
        // given
        ImportBatch batch = batch(1, List.of(row(2, NEWCOMER, RowStatus.INVALID)));
        given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));

        // when & then
        assertThatThrownBy(() -> facade.apply(1L, ASSISTANT))
            .isInstanceOf(ImportBatchHasInvalidRowsException.class);
        verify(enrollmentCommandService, never()).createEnrollment(any(), anyString(), any());
        verify(userCommandService, never()).createUser(any(), any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("apply는 두 번 반영할 수 없다")
    public void apply_RejectsSecondApply() {
        // given
        ImportBatch batch = batch(0, List.of(row(2, MEMBER, RowStatus.VALID)));
        given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));
        facade.apply(1L, ASSISTANT);

        // when & then
        assertThatThrownBy(() -> facade.apply(1L, ASSISTANT))
            .isInstanceOf(ImportBatchAlreadyAppliedException.class);
    }

    private EnrollmentImportRow row(int rowNumber, String studentNumber, RowStatus status) {
        return new EnrollmentImportRow(rowNumber, studentNumber, "이름",
            studentNumber + "@kyonggi.ac.kr", "010-0000-0000", Role.STUDENT, status, null);
    }

    private ImportBatch batch(int invalid, List<EnrollmentImportRow> rows) {
        return ImportBatch.builder()
            .id(1L)
            .uploadedBy(ASSISTANT)
            .sectionId(SECTION_ID)
            .type(Type.ENROLLMENT)
            .status(kgu.developers.domain.importBatch.domain.Status.PREVIEW)
            .payload(JsonConverter.toTree(rows))
            .summary(JsonConverter.toTree(new EnrollmentImportSummary(rows.size(), 0, 0, 0, invalid)))
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

    private User user(String studentNumber) {
        return User.create(studentNumber, studentNumber + "@kyonggi.ac.kr", "이름", "password",
            UserGlobalRole.USER, "010-0000-0000");
    }

    private MockMultipartFile excel(String[]... rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            String[][] all = new String[rows.length + 1][];
            all[0] = new String[] {"학번", "성명", "이메일", "연락처", "역할"};
            System.arraycopy(rows, 0, all, 1, rows.length);
            for (int i = 0; i < all.length; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < all[i].length; j++) {
                    row.createCell(j).setCellValue(all[i][j]);
                }
            }
            workbook.write(out);
            return new MockMultipartFile("file", "enrollments.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }
}
