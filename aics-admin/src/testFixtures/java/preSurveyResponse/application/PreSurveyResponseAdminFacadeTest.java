package preSurveyResponse.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.admin.preSurveyResponse.application.PreSurveyResponseAdminFacade;
import kgu.developers.admin.preSurveyResponse.presentation.response.PreSurveyResponseAdminListResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;

import mock.repository.FakePreSurveyResponseRepository;

class PreSurveyResponseAdminFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final String PROFESSOR = "professor1";
    private static final String OTHER_PROFESSOR = "professor2";

    private SectionQueryService sectionQueryService;
    private UserQueryService userQueryService;
    private FakePreSurveyResponseRepository preSurveyResponseRepository;
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

        userQueryService = mock(UserQueryService.class);
        given(userQueryService.getUsersByStudentNumbers(List.of("202412345"))).willReturn(List.of(
                User.create("202412345", "student@kyonggi.ac.kr", "김철수", "password", UserGlobalRole.USER, null)));
        given(userQueryService.getUsersByStudentNumbers(List.of())).willReturn(List.of());

        preSurveyResponseAdminFacade = new PreSurveyResponseAdminFacade(
                sectionQueryService, preSurveyResponseRepository, userQueryService);
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
}
