package course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.admin.course.application.CourseFacade;
import kgu.developers.admin.course.presentation.request.CourseRequest;
import kgu.developers.domain.course.application.command.CourseCommandService;
import kgu.developers.domain.course.application.query.CourseQueryService;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;

@ExtendWith(MockitoExtension.class)
class CourseFacadeTest {

  @Mock
  private CourseCommandService courseCommandService;

  @Mock
  private CourseQueryService courseQueryService;

  @InjectMocks
  private CourseFacade courseFacade;

  private final CourseRequest request =
      new CourseRequest("객체지향프로그래밍", 2026, SemesterType.FALL, StatusType.DRAFT);

  private Course course() {
    return Course.create("객체지향프로그래밍", 2026, SemesterType.FALL, StatusType.DRAFT);
  }

  @Test
  @DisplayName("createCourse는 요청 값을 커맨드 서비스에 넘기고 id를 응답한다")
  void createCourse() {
    given(courseCommandService.createCourse("객체지향프로그래밍", 2026, SemesterType.FALL, StatusType.DRAFT))
        .willReturn(1L);

    assertThat(courseFacade.createCourse(request).id()).isEqualTo(1L);
  }

  @Test
  @DisplayName("updateCourse는 조회한 강좌를 커맨드 서비스에 넘긴다")
  void updateCourse() {
    Course course = course();
    given(courseQueryService.getCourseById(1L)).willReturn(course);

    courseFacade.updateCourse(1L, request);

    verify(courseCommandService).updateCourse(course, "객체지향프로그래밍", 2026, SemesterType.FALL, StatusType.DRAFT);
  }

  @Test
  @DisplayName("deleteCourse는 조회한 강좌를 커맨드 서비스에 넘긴다")
  void deleteCourse() {
    Course course = course();
    given(courseQueryService.getCourseById(1L)).willReturn(course);

    courseFacade.deleteCourse(1L);

    verify(courseCommandService).deleteCourse(course);
  }

  @Test
  @DisplayName("getCourseById는 쿼리 서비스 결과를 그대로 반환한다")
  void getCourseById() {
    Course course = course();
    given(courseQueryService.getCourseById(1L)).willReturn(course);

    assertThat(courseFacade.getCourseById(1L)).isEqualTo(course);
  }

  @Test
  @DisplayName("getAllCourses는 목록을 응답으로 감싸 반환한다")
  void getAllCourses() {
    List<Course> courses = List.of(course());
    given(courseQueryService.getAllCourses()).willReturn(courses);

    assertThat(courseFacade.getAllCourses().contents()).isEqualTo(courses);
  }
}
