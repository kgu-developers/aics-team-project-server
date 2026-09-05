package course.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kgu.developers.admin.course.application.CourseFacade;
import kgu.developers.admin.course.presentation.CourseControllerImpl;
import kgu.developers.admin.course.presentation.response.CourseResponse;
import kgu.developers.common.exception.GlobalExceptionHandler;

@WebMvcTest
@Import({CourseControllerImpl.class, GlobalExceptionHandler.class})
@WithMockUser(roles = "ADMIN")
@TestPropertySource(properties = {
    "spring.security.user.name=admin",
    "spring.security.user.password=admin"
})
class CourseControllerValidationTest {

  private static final String COURSE_URL = "/api/v1/admin/courses/{id}";
  private static final String VALID_BODY = """
      {"name":"객체지향프로그래밍","year":2026,"semester":"FALL","status":"DRAFT"}
      """;

  @SpringBootConfiguration
  static class TestApp {
  }

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CourseFacade courseFacade;

  @Test
  @DisplayName("양수 id로 강좌를 조회하면 200을 응답한다")
  void getCourseByPositiveId() throws Exception {
    given(courseFacade.getCourseById(1L)).willReturn(CourseResponse.builder().id(1L).build());

    mockMvc.perform(get(COURSE_URL, 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @ParameterizedTest(name = "id={0}")
  @ValueSource(longs = {0L, -1L})
  @DisplayName("0 이하 id로 강좌를 조회하면 400을 응답한다")
  void getCourseByNonPositiveId(long id) throws Exception {
    mockMvc.perform(get(COURSE_URL, id))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

    then(courseFacade).should(never()).getCourseById(id);
  }

  @ParameterizedTest(name = "id={0}")
  @ValueSource(longs = {0L, -1L})
  @DisplayName("0 이하 id로 강좌를 수정하면 400을 응답한다")
  void updateCourseByNonPositiveId(long id) throws Exception {
    mockMvc.perform(put(COURSE_URL, id).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_BODY))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

    then(courseFacade).shouldHaveNoInteractions();
  }

  @ParameterizedTest(name = "id={0}")
  @ValueSource(longs = {0L, -1L})
  @DisplayName("0 이하 id로 강좌를 삭제하면 400을 응답한다")
  void deleteCourseByNonPositiveId(long id) throws Exception {
    mockMvc.perform(delete(COURSE_URL, id).with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

    then(courseFacade).should(never()).deleteCourse(id);
  }
}
