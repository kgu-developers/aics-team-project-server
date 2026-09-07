package preSurveyResponse.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.ByteArrayInputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.admin.preSurveyResponse.application.PreSurveyResponseAdminFacade;
import kgu.developers.admin.preSurveyResponse.application.PreSurveyResponseExcelDownload;
import kgu.developers.admin.preSurveyResponse.presentation.response.PreSurveyResponseAdminListResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseQueryService;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;

import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakePreSurveyResponseRepository;
import mock.repository.FakeUserRepository;

class PreSurveyResponseAdminFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final String PROFESSOR = "professor1";
    private static final String OTHER_PROFESSOR = "professor2";

    private SectionQueryService sectionQueryService;
    private UserQueryService userQueryService;
    private FakePreSurveyResponseRepository preSurveyResponseRepository;
    private FakeEnrollmentRepository enrollmentRepository;
    private PreSurveyResponseAdminFacade preSurveyResponseAdminFacade;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        sectionQueryService = mock(SectionQueryService.class);
        preSurveyResponseRepository = new FakePreSurveyResponseRepository();

        JsonNode roles = objectMapper.readTree("[\"BACKEND\", \"PM\"]");
        preSurveyResponseRepository.save(
                PreSurveyResponse.create("202412345", SECTION_ID, roles, "학사 알림 서비스", "금요일 회의 어려움", null));

        enrollmentRepository = new FakeEnrollmentRepository();
        enrollmentRepository.save(Enrollment.create(SECTION_ID, "202412345", Role.STUDENT, Status.ACTIVE));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, "202498765", Role.STUDENT, Status.ACTIVE));  // 미응답
        enrollmentRepository.save(Enrollment.create(SECTION_ID, "202400001", Role.ASSISTANT, Status.ACTIVE));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, "202400002", Role.STUDENT, Status.WITHDRAWN));

        FakeUserRepository userRepository = new FakeUserRepository();
        userRepository.save(User.builder().studentNumber("202412345").name("이석민").build());
        userRepository.save(User.builder().studentNumber("202498765").name("김철수").build());

        userQueryService = mock(UserQueryService.class);
        given(userQueryService.getUsersByStudentNumbers(List.of("202412345"))).willReturn(List.of(
                User.create("202412345", "student@kyonggi.ac.kr", "김철수", "password", UserGlobalRole.USER, null)));
        given(userQueryService.getUsersByStudentNumbers(List.of())).willReturn(List.of());

        preSurveyResponseAdminFacade = new PreSurveyResponseAdminFacade(sectionQueryService, preSurveyResponseRepository,
                userQueryService,
                new PreSurveyResponseQueryService(preSurveyResponseRepository, enrollmentRepository, userRepository));
    }

    @Test
    @DisplayName("담당 교수는 분반 사전조사 응답 목록을 조회할 수 있다")
    void getResponsesBySection_AllowsOwningProfessor() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        PreSurveyResponseAdminListResponse response = preSurveyResponseAdminFacade.getResponsesBySection(SECTION_ID, PROFESSOR);

        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().get(0).userId()).isEqualTo("202412345");
        assertThat(response.contents().get(0).userName()).isEqualTo("김철수");
        assertThat(response.contents().get(0).topicOpinion()).isEqualTo("학사 알림 서비스");
    }

    @Test
    @DisplayName("응답 후 탈퇴한 학생은 이름 자리에 대체 문구가 들어간다")
    void getResponsesBySection_FillsNameForWithdrawnUser() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(userQueryService.getUsersByStudentNumbers(List.of("202412345"))).willReturn(List.of());

        PreSurveyResponseAdminListResponse response = preSurveyResponseAdminFacade.getResponsesBySection(SECTION_ID, PROFESSOR);

        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().get(0).userName()).isEqualTo("(탈퇴한 사용자)");
    }

    @Test
    @DisplayName("담당 교수가 아니면 분반 사전조사 응답 목록을 조회할 수 없다")
    void getResponsesBySection_RejectsNonOwningProfessor() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, OTHER_PROFESSOR)).willReturn(false);

        assertThatThrownBy(() -> preSurveyResponseAdminFacade.getResponsesBySection(SECTION_ID, OTHER_PROFESSOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("아직 아무도 응답하지 않은 분반은 빈 목록을 반환한다")
    void getResponsesBySection_ReturnsEmptyWhenNoResponses() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, PROFESSOR)).willReturn(true);

        PreSurveyResponseAdminListResponse response = preSurveyResponseAdminFacade.getResponsesBySection(2L, PROFESSOR);

        assertThat(response.contents()).isEmpty();
    }

    @Test
    @DisplayName("서로 지목하거나 한쪽이 수락한 경우 mutual 이 true다")
    void getResponsesBySection_MarksMutualNominations() throws Exception {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, PROFESSOR)).willReturn(true);
        JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");
        // A <-> B 는 서로 지목, C 는 A 를 지목하고 A 가 수락, D 는 A 를 짝사랑
        preSurveyResponseRepository.save(PreSurveyResponse.create("A", Long.valueOf(2L), roles, null, null, "B"));
        preSurveyResponseRepository.save(PreSurveyResponse.create("B", Long.valueOf(2L), roles, null, null, "A"));
        PreSurveyResponse responseC = preSurveyResponseRepository.save(PreSurveyResponse.create("C", Long.valueOf(2L), roles, null, null, "A"));
        responseC.decidePreferredPeer(true);
        preSurveyResponseRepository.save(responseC);
        preSurveyResponseRepository.save(PreSurveyResponse.create("D", Long.valueOf(2L), roles, null, null, "A"));
        given(userQueryService.getUsersByStudentNumbers(List.of("A", "B", "C", "D"))).willReturn(List.of(
                User.create("A", "a@kyonggi.ac.kr", "김철수", "password", UserGlobalRole.USER, null),
                User.create("B", "b@kyonggi.ac.kr", "이영희", "password", UserGlobalRole.USER, null),
                User.create("C", "c@kyonggi.ac.kr", "박민수", "password", UserGlobalRole.USER, null),
                User.create("D", "d@kyonggi.ac.kr", "최수진", "password", UserGlobalRole.USER, null)));

        PreSurveyResponseAdminListResponse response = preSurveyResponseAdminFacade.getResponsesBySection(2L, PROFESSOR);

        assertThat(response.contents())
                .extracting(r -> r.userId() + ":" + r.preferredPeerName() + ":" + r.mutual())
                .containsExactly("A:이영희:true", "B:김철수:true", "C:김철수:true", "D:김철수:false");
    }

    @Test
    @DisplayName("서로 지목했지만 한쪽이 거절하면 mutual 이 false다")
    void getResponsesBySection_RejectionBreaksMutualNominations() throws Exception {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, PROFESSOR)).willReturn(true);
        JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");
        // A <-> B 는 서로 지목하지만 B 가 A 를 거절, C 는 A 를 지목하고 A 가 C 를 거절
        PreSurveyResponse responseA = preSurveyResponseRepository.save(PreSurveyResponse.create("A", Long.valueOf(2L), roles, null, null, "B"));
        PreSurveyResponse responseB = preSurveyResponseRepository.save(PreSurveyResponse.create("B", Long.valueOf(2L), roles, null, null, "A"));
        responseB.decidePreferredPeer(false); // B가 A를 거절
        preSurveyResponseRepository.save(responseB);
        PreSurveyResponse responseC = preSurveyResponseRepository.save(PreSurveyResponse.create("C", Long.valueOf(2L), roles, null, null, "A"));
        responseA.decidePreferredPeer(false); // A가 C를 거절
        preSurveyResponseRepository.save(responseA);
        preSurveyResponseRepository.save(responseC);
        given(userQueryService.getUsersByStudentNumbers(List.of("A", "B", "C"))).willReturn(List.of(
                User.create("A", "a@kyonggi.ac.kr", "김철수", "password", UserGlobalRole.USER, null),
                User.create("B", "b@kyonggi.ac.kr", "이영희", "password", UserGlobalRole.USER, null),
                User.create("C", "c@kyonggi.ac.kr", "박민수", "password", UserGlobalRole.USER, null)));

        PreSurveyResponseAdminListResponse response = preSurveyResponseAdminFacade.getResponsesBySection(2L, PROFESSOR);

        assertThat(response.contents())
                .extracting(r -> r.userId() + ":" + r.preferredPeerName() + ":" + r.mutual())
                .containsExactly("A:이영희:false", "B:김철수:false", "C:김철수:false");
    }

    @Test
    @DisplayName("담당 교수는 분반 사전조사 응답을 엑셀로 다운로드할 수 있다")
    void downloadResponsesExcel_WritesHeaderAndRows() throws Exception {
        given(sectionQueryService.getSectionById(SECTION_ID)).willReturn(sectionOwnedBy(PROFESSOR));

        PreSurveyResponseExcelDownload download = preSurveyResponseAdminFacade.downloadResponsesExcel(SECTION_ID, PROFESSOR);

        assertThat(download.fileName()).isEqualTo("월1,2_CS101-사전조사.xlsx");
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download.content()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("학번");

            Row submitted = sheet.getRow(1);
            assertThat(submitted.getCell(0).getStringCellValue()).isEqualTo("202412345");
            assertThat(submitted.getCell(1).getStringCellValue()).isEqualTo("이석민");
            assertThat(submitted.getCell(2).getStringCellValue()).isEqualTo("BACKEND, PM");
            assertThat(submitted.getCell(3).getStringCellValue()).isEqualTo("학사 알림 서비스");
            assertThat(submitted.getCell(4).getStringCellValue()).isEqualTo("금요일 회의 어려움");

            // 미응답 수강생은 학번·이름만 있는 행 + "미제출"로 들어가고, 조교·탈퇴 수강생은 빠진다
            Row notSubmitted = sheet.getRow(2);
            assertThat(notSubmitted.getCell(0).getStringCellValue()).isEqualTo("202498765");
            assertThat(notSubmitted.getCell(1).getStringCellValue()).isEqualTo("김철수");
            assertThat(notSubmitted.getCell(2)).isNull();
            assertThat(notSubmitted.getCell(5).getStringCellValue()).isEqualTo("미제출");
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("담당 교수가 아니면 엑셀을 다운로드할 수 없다")
    void downloadResponsesExcel_RejectsNonOwningProfessor() {
        given(sectionQueryService.getSectionById(SECTION_ID)).willReturn(sectionOwnedBy(PROFESSOR));

        assertThatThrownBy(() -> preSurveyResponseAdminFacade.downloadResponsesExcel(SECTION_ID, OTHER_PROFESSOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("엑셀 다운로드는 분반을 한 번만 조회한다")
    void downloadResponsesExcel_ReadsSectionOnce() {
        given(sectionQueryService.getSectionById(SECTION_ID)).willReturn(sectionOwnedBy(PROFESSOR));

        preSurveyResponseAdminFacade.downloadResponsesExcel(SECTION_ID, PROFESSOR);

        verify(sectionQueryService, times(1)).getSectionById(SECTION_ID);
        verifyNoMoreInteractions(sectionQueryService);
    }

    private SectionDetail sectionOwnedBy(String professorId) {
        return new SectionDetail(
                Section.builder().id(SECTION_ID).professorId(professorId)
                        .classTime("월1,2").code("CS101").build(),
                null, null);
    }
}
