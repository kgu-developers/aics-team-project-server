package section.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import kgu.developers.api.section.application.SectionFacade;
import kgu.developers.api.section.presentation.SectionControllerImpl;
import kgu.developers.api.section.presentation.response.SectionListResponse;
import kgu.developers.api.section.presentation.response.SectionResponse;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;

@ExtendWith(MockitoExtension.class)
class SectionControllerTest {

    private static final String BASE_URL = "/api/v1/sections";
    private static final String STUDENT_NUMBER = "202699999";

    @Mock
    private SectionFacade sectionFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SectionControllerImpl(sectionFacade)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(STUDENT_NUMBER, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SectionResponse response() {
        Section section = Section.builder()
                .id(1L)
                .professorId(STUDENT_NUMBER)
                .courseId(1L)
                .code("CS101")
                .name("01분반")
                .classTime("월3,4")
                .capacity(40)
                .contactVisibleFrom(LocalDateTime.of(2026, 3, 2, 0, 0))
                .contactVisibleUntil(LocalDateTime.of(2026, 6, 20, 18, 0))
                .build();
        Course course = Course.builder()
                .id(1L)
                .name("객체지향프로그래밍")
                .year(2026)
                .semester(SemesterType.SPRING)
                .status(StatusType.ACTIVE)
                .build();
        User professor = User.create(STUDENT_NUMBER, "kgu@kgu.ac.kr", "김교수", "encoded",
                UserGlobalRole.USER, "010-0000-0000");
        return SectionResponse.from(new SectionDetail(section, course, professor));
    }

    @Test
    @DisplayName("GET /sections는 토큰의 학번으로 본인 분반 목록을 조회한다")
    void getMySectionsUsesTokenStudentNumber() throws Exception {
        given(sectionFacade.getMySections(STUDENT_NUMBER, null, null, null))
                .willReturn(new SectionListResponse(List.of(response())));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents[0].id").value(1L))
                .andExpect(jsonPath("$.contents[0].courseName").value("객체지향프로그래밍"));
    }

    @Test
    @DisplayName("GET /sections는 status/year/semester 조건을 그대로 전달한다")
    void getMySectionsPassesFilters() throws Exception {
        given(sectionFacade.getMySections(STUDENT_NUMBER, StatusType.ACTIVE, 2026, SemesterType.SPRING))
                .willReturn(new SectionListResponse(List.of(response())));

        mockMvc.perform(get(BASE_URL)
                        .param("status", "ACTIVE")
                        .param("year", "2026")
                        .param("semester", "SPRING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents").isNotEmpty());
    }

    @Test
    @DisplayName("GET /sections/{sectionId}는 200과 분반 상세를 응답한다")
    void getSectionById() throws Exception {
        given(sectionFacade.getSectionById(1L)).willReturn(response());

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CS101"))
                .andExpect(jsonPath("$.semester").value("SPRING"));
    }
}
