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
import org.springframework.security.access.AccessDeniedException;

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
import kgu.developers.admin.importcommon.SectionStaffValidator;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.domain.importBatch.domain.ImportBatchRepository;
import kgu.developers.domain.importBatch.domain.Type;
import kgu.developers.domain.importBatch.exception.ImportBatchAlreadyAppliedException;
import kgu.developers.domain.importBatch.exception.ImportBatchFileInvalidException;
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
    private static final String ENROLLED_BUT_NOT_YET = "202400004";

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
            sectionRepository,
            new SectionStaffValidator(enrollmentRepository, sectionRepository, userRepository));

        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(mock(SectionDetail.class)));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, ASSISTANT))
            .willReturn(Optional.of(Enrollment.create(SECTION_ID, ASSISTANT, Role.ASSISTANT, Status.ACTIVE)));
        given(enrollmentRepository.findAllBySectionId(SECTION_ID))
            .willReturn(List.of(Enrollment.create(SECTION_ID, ENROLLED, Role.STUDENT, Status.ACTIVE)));
        given(userRepository.findAllByStudentNumberIn(any()))
            .willReturn(List.of(user(MEMBER), user(ENROLLED)));
        given(userRepository.findAllIncludingDeletedByStudentNumberIn(any()))
            .willReturn(List.of(user(MEMBER), user(ENROLLED)));
        given(userRepository.findAllByEmailIn(any())).willReturn(List.of());
        given(userRepository.findAllIncludingDeletedByEmailIn(any())).willReturn(List.of());
        given(importBatchRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 1L));
        given(sectionRepository.findActiveByIdForUpdate(SECTION_ID))
            .willReturn(Optional.of(mock(Section.class)));
    }

    @Test
    @DisplayName("preview는 행별로 상태를 분류하고 요약을 만든다")
    public void preview_ClassifiesRows() throws IOException {
        // given
        MockMultipartFile file = excel(
            new String[] {MEMBER, "홍길동", "hong@kyonggi.ac.kr", "010-0000-0001", ""},
            new String[] {ENROLLED, "김철수", "kim@kyonggi.ac.kr", "010-0000-0002", "학생"},
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
        assertThat(rows.get(1).role()).isEqualTo(Role.STUDENT);
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
    @DisplayName("preview는 같은 이메일을 가진 유저가 여러 명이어도 죽지 않는다")
    public void preview_SurvivesDuplicateEmailOwners() throws IOException {
        // given
        String email = "dup@kyonggi.ac.kr";
        given(userRepository.findAllIncludingDeletedByEmailIn(any())).willReturn(List.of(
            User.create("202400010", email, "탈퇴자1", "password", UserGlobalRole.USER, "010-0000-0010"),
            User.create("202400011", email, "탈퇴자2", "password", UserGlobalRole.USER, "010-0000-0011")));

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT,
            excel(new String[] {NEWCOMER, "이영희", email, "010-0000-0003", ""}));

        // then
        assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.INVALID);
    }

    @Test
    @DisplayName("preview는 신규 가입 대상의 연락처가 비면 오류로 표시한다")
    public void preview_RejectsNewUserWithoutPhone() throws IOException {
        // given
        MockMultipartFile file = excel(new String[] {NEWCOMER, "이영희", "lee@kyonggi.ac.kr", "", ""});

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT, file);

        // then
        assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.INVALID);
        assertThat(response.rows().get(0).message()).isEqualTo("신규 가입 대상은 연락처가 필요합니다.");
    }

    @Test
    @DisplayName("preview는 탈퇴 이력이 있는 학번을 오류로 표시한다")
    public void preview_RejectsWithdrawnUser() throws IOException {
        // given
        given(userRepository.findAllIncludingDeletedByStudentNumberIn(any()))
            .willReturn(List.of(user(NEWCOMER)));
        given(userRepository.findAllByEmailIn(any())).willReturn(List.of());
        given(userRepository.findAllIncludingDeletedByEmailIn(any())).willReturn(List.of());

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
    @DisplayName("preview는 탈퇴한 사용자가 사용했던 이메일을 오류로 표시한다")
    public void preview_RejectsEmailFromDeletedUser() throws IOException {
        // given
        given(userRepository.findAllByEmailIn(any())).willReturn(List.of());
        given(userRepository.findAllIncludingDeletedByEmailIn(any())).willReturn(List.of(user(MEMBER)));
        given(userRepository.findAllIncludingDeletedByStudentNumberIn(any()))
            .willReturn(List.of(user(MEMBER), user(ENROLLED)));

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT,
            excel(new String[] {NEWCOMER, "이영희", MEMBER + "@kyonggi.ac.kr", "010-0000-0003", ""}));

        // then
        assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.INVALID);
        assertThat(response.rows().get(0).message()).contains("탈퇴한 사용자가 사용했던 이메일");
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
        given(enrollmentRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(
            Enrollment.create(SECTION_ID, MEMBER, Role.STUDENT, Status.WITHDRAWN)));

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
    @DisplayName("preview는 그 분반 조교도 담당 교수도 관리자도 아니면 거부한다")
    public void preview_RejectsNonStaff() throws IOException {
        // when & then
        assertThatThrownBy(() -> facade.preview(SECTION_ID, MEMBER,
            excel(new String[] {MEMBER, "홍길동", "hong@kyonggi.ac.kr", "010-0000-0001", ""})))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("preview는 전역 관리자면 분반 조교가 아니어도 허용한다")
    public void preview_AllowsGlobalAdmin() throws IOException {
        // given
        given(userRepository.findByStudentNumber(MEMBER)).willReturn(Optional.of(
            User.create(MEMBER, MEMBER + "@kyonggi.ac.kr", "관리자", "password",
                UserGlobalRole.ADMIN, "010-0000-0000")));

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, MEMBER,
            excel(new String[] {MEMBER, "홍길동", "hong@kyonggi.ac.kr", "010-0000-0001", ""}));

        // then
        assertThat(response.importId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("preview는 없는 분반이면 거부한다")
    public void preview_RejectsUnknownSection() throws IOException {
        // given
        String admin = "202499998";
        given(userRepository.findByStudentNumber(admin)).willReturn(Optional.of(
            User.create(admin, admin + "@kyonggi.ac.kr", "관리자", "password",
                UserGlobalRole.ADMIN, "010-0000-0000")));
        given(sectionRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> facade.preview(999L, admin,
            excel(new String[] {MEMBER, "홍길동", "hong@kyonggi.ac.kr", "010-0000-0001", ""})))
            .isInstanceOf(SectionNotFoundException.class);
    }

    @Test
    @DisplayName("preview는 담당자가 아니면 분반 존재 여부를 알려주지 않는다")
    public void preview_HidesSectionExistenceFromNonStaff() throws IOException {
        // given
        given(sectionRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> facade.preview(999L, MEMBER,
            excel(new String[] {MEMBER, "홍길동", "hong@kyonggi.ac.kr", "010-0000-0001", ""})))
            .isInstanceOf(AccessDeniedException.class);
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
        // ENROLLED 행은 DUPLICATE라 반영되지 않고, skipped 필드("이미 등록되어 건너뛴 수")에 잡혀야 한다
        assertThat(response.skipped()).isEqualTo(1);
        verify(userCommandService).createUser(NEWCOMER, NEWCOMER + "@kyonggi.ac.kr", "이름", "010-0000-0000",
            UserGlobalRole.USER, "010-0000-0000", false);
        verify(userCommandService, never()).createUser(eq(MEMBER), any(), any(), any(), any(), any(), anyBoolean());
        verify(enrollmentCommandService).createEnrollment(SECTION_ID, MEMBER, Role.STUDENT);
        verify(enrollmentCommandService).createEnrollment(SECTION_ID, NEWCOMER, Role.STUDENT);
        verify(enrollmentCommandService, never()).createEnrollment(eq(SECTION_ID), eq(ENROLLED), any());
    }

    @Test
    @DisplayName("apply는 preview 이후 계정이 탈퇴한 학생은 그 행만 건너뛰고 나머지는 반영한다")
    public void apply_SkipsRowWhenUserWithdrawnAfterPreview() {
        // given: preview 때는 존재했던 MEMBER 계정이 apply 시점엔 탈퇴해 createEnrollment가 실패한다
        ImportBatch batch = batch(0, List.of(
            row(2, MEMBER, RowStatus.VALID),
            row(3, ENROLLED_BUT_NOT_YET, RowStatus.VALID)));
        given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));
        given(enrollmentCommandService.createEnrollment(SECTION_ID, MEMBER, Role.STUDENT))
            .willThrow(new kgu.developers.domain.user.exception.UserNotFoundException());

        // when
        EnrollmentImportApplyResponse response = facade.apply(1L, ASSISTANT);

        // then: MEMBER 행만 건너뛰고, 트랜잭션 전체가 롤백되지 않고 나머지 행은 반영된다
        assertThat(response.applied()).isEqualTo(1);
        assertThat(response.skipped()).isEqualTo(1);
        verify(enrollmentCommandService).createEnrollment(SECTION_ID, ENROLLED_BUT_NOT_YET, Role.STUDENT);
    }

    @Test
    @DisplayName("apply는 분반에 쓰기 락을 걸어 동시 반영을 직렬화한다")
    public void apply_LocksSection() {
        // given
        given(importBatchRepository.findById(1L))
            .willReturn(Optional.of(batch(0, List.of(row(2, MEMBER, RowStatus.VALID)))));

        // when
        facade.apply(1L, ASSISTANT);

        // then
        verify(sectionRepository).findActiveByIdForUpdate(SECTION_ID);
    }

    @Test
    @DisplayName("apply는 미리보기 이후 분반이 삭제됐으면 거부한다")
    public void apply_RejectsDeletedSection() {
        // given
        given(sectionRepository.findActiveByIdForUpdate(SECTION_ID)).willReturn(Optional.empty());
        given(importBatchRepository.findById(1L))
            .willReturn(Optional.of(batch(0, List.of(row(2, MEMBER, RowStatus.VALID)))));

        // when & then
        assertThatThrownBy(() -> facade.apply(1L, ASSISTANT))
            .isInstanceOf(SectionNotFoundException.class);
        verify(enrollmentCommandService, never()).createEnrollment(any(), any(), any());
    }

    @Test
    @DisplayName("apply는 연락처가 빈 신규 가입 행을 쓰기 직전에 건너뛴다")
    public void apply_SkipsNewUserWithoutPhone() {
        // given: preview에 연락처 검사가 없던 시점에 만들어진 배치
        EnrollmentImportRow noPhone = new EnrollmentImportRow(2, NEWCOMER, "이영희",
            NEWCOMER + "@kyonggi.ac.kr", "", Role.STUDENT, RowStatus.NEW_USER, null);
        given(importBatchRepository.findById(1L))
            .willReturn(Optional.of(batch(0, List.of(noPhone))));

        // when
        EnrollmentImportApplyResponse response = facade.apply(1L, ASSISTANT);

        // then
        assertThat(response.createdUsers()).isZero();
        assertThat(response.applied()).isZero();
        assertThat(response.skipped()).isEqualTo(1);
        verify(userCommandService, never()).createUser(any(), any(), any(), any(), any(), any(), anyBoolean());
        verify(enrollmentCommandService, never()).createEnrollment(any(), any(), any());
    }

    @Test
    @DisplayName("apply는 미리보기 이후 등록된 학생을 건너뛴다")
    public void apply_SkipsAlreadyEnrolled() {
        // given
        ImportBatch batch = batch(0, List.of(row(2, MEMBER, RowStatus.VALID)));
        given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));
        given(enrollmentRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(
            Enrollment.create(SECTION_ID, MEMBER, Role.STUDENT, Status.ACTIVE)));

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

    @Test
    @DisplayName("preview는 조교가 조교 역할을 지정하면 거부한다")
    public void preview_RejectsAssistantRoleByAssistant() throws IOException {
        // given
        MockMultipartFile file = excel(
            new String[] {NEWCOMER, "이영희", "lee@kyonggi.ac.kr", "010-0000-0003", "조교"}
        );

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT, file);

        // then
        assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.INVALID);
        assertThat(response.rows().get(0).message()).contains("조교는 학생 역할만 지정할 수 있습니다.");
    }

    @Test
    @DisplayName("preview는 조교가 학생 역할을 지정하면 허용한다")
    public void preview_AllowsStudentRoleByAssistant() throws IOException {
        // given
        MockMultipartFile file = excel(
            new String[] {NEWCOMER, "이영희", "lee@kyonggi.ac.kr", "010-0000-0003", "학생"}
        );

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, ASSISTANT, file);

        // then
        assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.NEW_USER);
    }

    @Test
    @DisplayName("preview는 관리자가 조교 역할을 지정하면 허용한다")
    public void preview_AllowsAssistantRoleByAdmin() throws IOException {
        // given
        String admin = "202499998";
        given(userRepository.findByStudentNumber(admin)).willReturn(Optional.of(
            User.create(admin, admin + "@kyonggi.ac.kr", "관리자", "password",
                UserGlobalRole.ADMIN, "010-0000-0000")));
        
        MockMultipartFile file = excel(
            new String[] {NEWCOMER, "이영희", "lee@kyonggi.ac.kr", "010-0000-0003", "조교"}
        );

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, admin, file);

        // then
        assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.NEW_USER);
    }

    @Test
    @DisplayName("preview는 담당 교수가 조교 역할을 지정하면 허용한다")
    public void preview_AllowsAssistantRoleByProfessor() throws IOException {
        // given
        String professor = "202499997";
        given(sectionRepository.existsActiveByIdAndProfessorId(SECTION_ID, professor)).willReturn(true);
        
        MockMultipartFile file = excel(
            new String[] {NEWCOMER, "이영희", "lee@kyonggi.ac.kr", "010-0000-0003", "조교"}
        );

        // when
        EnrollmentImportPreviewResponse response = facade.preview(SECTION_ID, professor, file);

        // then
        assertThat(response.rows().get(0).status()).isEqualTo(RowStatus.NEW_USER);
    }

    @Test
    @DisplayName("apply는 조교가 조교 역할을 지정하면 건너뛴다")
    public void apply_SkipsAssistantRoleByAssistant() {
        // given
        ImportBatch batch = batch(0, List.of(
            row(2, NEWCOMER, RowStatus.VALID, Role.ASSISTANT),
            row(3, MEMBER, RowStatus.VALID, Role.STUDENT)
        ));
        given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));

        // when
        EnrollmentImportApplyResponse response = facade.apply(1L, ASSISTANT);

        // then
        assertThat(response.applied()).isEqualTo(1);
        assertThat(response.skipped()).isEqualTo(1);
        verify(enrollmentCommandService).createEnrollment(SECTION_ID, MEMBER, Role.STUDENT);
        verify(enrollmentCommandService, never()).createEnrollment(eq(SECTION_ID), eq(NEWCOMER), eq(Role.ASSISTANT));
    }

    @Test
    @DisplayName("apply는 관리자가 조교 역할을 지정하면 허용한다")
    public void apply_AllowsAssistantRoleByAdmin() {
        // given
        String admin = "202499998";
        given(userRepository.findByStudentNumber(admin)).willReturn(Optional.of(
            User.create(admin, admin + "@kyonggi.ac.kr", "관리자", "password",
                UserGlobalRole.ADMIN, "010-0000-0000")));
        
        ImportBatch batch = batch(0, List.of(
            row(2, NEWCOMER, RowStatus.VALID, Role.ASSISTANT)
        ));
        given(importBatchRepository.findById(1L)).willReturn(Optional.of(batch));

        // when
        EnrollmentImportApplyResponse response = facade.apply(1L, admin);

        // then
        assertThat(response.applied()).isEqualTo(1);
        assertThat(response.skipped()).isZero();
        verify(enrollmentCommandService).createEnrollment(SECTION_ID, NEWCOMER, Role.ASSISTANT);
    }

    @Test
    @DisplayName("preview는 10MB를 초과하는 파일을 거부한다")
    public void preview_RejectsLargeFile() throws IOException {
        // given
        MockMultipartFile largeFile = new MockMultipartFile("file", "enrollments.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[10 * 1024 * 1024 + 1]); // 10MB + 1 byte

        // when & then
        assertThatThrownBy(() -> facade.preview(SECTION_ID, ASSISTANT, largeFile))
            .isInstanceOf(ImportBatchFileInvalidException.class)
            .cause().isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("파일 크기가 10MB를 초과했습니다.");
    }

    @Test
    @DisplayName("preview는 1000행을 초과하는 파일을 거부한다")
    public void preview_RejectsTooManyRows() throws IOException {
        // given
        String[][] manyRows = new String[1001][];
        for (int i = 0; i < 1001; i++) {
            manyRows[i] = new String[] {String.format("2021%04d", i), "학생" + i,
                String.format("2021%04d@kyonggi.ac.kr", i), "010-1234-5678", "학생"};
        }
        MockMultipartFile file = excel(manyRows);

        // when & then
        assertThatThrownBy(() -> facade.preview(SECTION_ID, ASSISTANT, file))
            .isInstanceOf(ImportBatchFileInvalidException.class)
            .cause().isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("행 수가 1000행을 초과했습니다.");
    }

    private EnrollmentImportRow row(int rowNumber, String studentNumber, RowStatus status) {
        return new EnrollmentImportRow(rowNumber, studentNumber, "이름",
            studentNumber + "@kyonggi.ac.kr", "010-0000-0000", Role.STUDENT, status, null);
    }

    private EnrollmentImportRow row(int rowNumber, String studentNumber, RowStatus status, Role role) {
        return new EnrollmentImportRow(rowNumber, studentNumber, "이름",
            studentNumber + "@kyonggi.ac.kr", "010-0000-0000", role, status, null);
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
