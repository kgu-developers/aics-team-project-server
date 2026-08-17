package section.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import kgu.developers.admin.section.application.SectionAdminFacade;
import kgu.developers.admin.section.presentation.SectionAdminControllerImpl;
import kgu.developers.admin.section.presentation.request.SectionAdminRequest;
import kgu.developers.admin.section.presentation.request.SectionAdminUpdateRequest;
import kgu.developers.admin.section.presentation.request.SectionContactVisibilityUpdateRequest;
import kgu.developers.admin.section.presentation.response.SectionAdminListResponse;
import kgu.developers.admin.section.presentation.response.SectionAdminPersistResponse;
import kgu.developers.admin.section.presentation.response.SectionAdminResponse;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;

@ExtendWith(MockitoExtension.class)
class SectionAdminControllerTest {

    private static final String BASE_URL = "/api/v1/admin/oop/sections";
    private static final String PROFESSOR_ID = "202699999";
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 3, 2, 0, 0);
    private static final LocalDateTime UNTIL = LocalDateTime.of(2026, 6, 20, 18, 0);

    @Mock
    private SectionAdminFacade sectionAdminFacade;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SectionAdminControllerImpl(sectionAdminFacade)).build();
    }

    private SectionAdminResponse response() {
        Section section = Section.builder()
                .id(1L)
                .professorId(PROFESSOR_ID)
                .courseId(1L)
                .code("CS101")
                .name("01분반")
                .classTime("월3,4")
                .capacity(40)
                .contactVisibleFrom(FROM)
                .contactVisibleUntil(UNTIL)
                .build();
        Course course = Course.builder()
                .id(1L)
                .name("객체지향프로그래밍")
                .year(2026)
                .semester(SemesterType.SPRING)
                .status(StatusType.ACTIVE)
                .build();
        User professor = User.create(PROFESSOR_ID, "kgu@kgu.ac.kr", "김교수", "encoded",
                UserGlobalRole.USER, "010-0000-0000");
        return SectionAdminResponse.from(new SectionDetail(section, course, professor));
    }

    @Test
    @DisplayName("POST /sections는 201과 생성된 분반 ID를 응답한다")
    void createSection() throws Exception {
        SectionAdminRequest request =
                new SectionAdminRequest(PROFESSOR_ID, 1L, "CS101", "01분반", "월3,4", 40, FROM, UNTIL);
        given(sectionAdminFacade.createSection(request)).willReturn(SectionAdminPersistResponse.of(1L));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /sections/{sectionId}는 200과 분반 상세를 응답한다")
    void getSectionById() throws Exception {
        given(sectionAdminFacade.getSectionsById(1L)).willReturn(response());

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CS101"))
                .andExpect(jsonPath("$.professor.studentNumber").value(PROFESSOR_ID))
                .andExpect(jsonPath("$.course.name").value("객체지향프로그래밍"));
    }

    @Test
    @DisplayName("GET /sections?courseId=는 강좌별 목록으로 라우팅된다")
    void getSectionsByCourseId() throws Exception {
        given(sectionAdminFacade.getSectionsByCourseId(1L))
                .willReturn(SectionAdminListResponse.builder().contents(List.of(response())).build());

        mockMvc.perform(get(BASE_URL).param("courseId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /sections?professorId=는 교수별 목록으로 라우팅되고 조건은 그대로 전달된다")
    void getSectionsByProfessorId() throws Exception {
        given(sectionAdminFacade.getSectionsByProfessorId(PROFESSOR_ID, StatusType.ACTIVE, 2026, SemesterType.SPRING))
                .willReturn(SectionAdminListResponse.builder().contents(List.of(response())).build());

        mockMvc.perform(get(BASE_URL)
                        .param("professorId", PROFESSOR_ID)
                        .param("status", "ACTIVE")
                        .param("year", "2026")
                        .param("semester", "SPRING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents[0].id").value(1L));
    }

    @Test
    @DisplayName("PATCH /sections/{sectionId}는 200과 수정된 분반을 응답한다")
    void updateSection() throws Exception {
        SectionAdminUpdateRequest request =
                new SectionAdminUpdateRequest(null, null, null, "02분반", null, null);
        given(sectionAdminFacade.updateSection(eq(1L), eq(request))).willReturn(response());

        mockMvc.perform(patch(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PATCH /sections/{sectionId}/contact-visibility는 200과 수정된 분반을 응답한다")
    void updateSectionContactVisibility() throws Exception {
        SectionContactVisibilityUpdateRequest request = new SectionContactVisibilityUpdateRequest(FROM, UNTIL);
        given(sectionAdminFacade.updateSectionContactVisibility(1L, request)).willReturn(response());

        mockMvc.perform(patch(BASE_URL + "/1/contact-visibility")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(sectionAdminFacade).updateSectionContactVisibility(1L, request);
    }

    @Test
    @DisplayName("DELETE /sections/{sectionId}는 204를 응답한다")
    void deleteSection() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());

        verify(sectionAdminFacade).deleteSection(1L);
    }
}
