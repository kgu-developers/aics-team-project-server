package course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.course.application.command.CourseCommandService;
import kgu.developers.domain.course.application.query.CourseQueryService;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.CourseRepository;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.exception.CourseNotFoundException;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

  @Mock
  private CourseRepository courseRepository;

  @InjectMocks
  private CourseCommandService commandService;

  @InjectMocks
  private CourseQueryService queryService;

  private Course course(Long id) {
    return Course.builder()
        .id(id)
        .name("객체지향프로그래밍")
        .year(2026)
        .semester(SemesterType.FALL)
        .status(StatusType.DRAFT)
        .build();
  }

  @Test
  @DisplayName("createCourse는 저장된 강좌의 id를 반환한다")
  void createCourse() {
    given(courseRepository.save(any(Course.class))).willReturn(course(1L));

    Long id = commandService.createCourse("객체지향프로그래밍", 2026, SemesterType.FALL, StatusType.DRAFT);

    assertThat(id).isEqualTo(1L);

    ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
    verify(courseRepository).save(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("객체지향프로그래밍");
    assertThat(captor.getValue().getYear()).isEqualTo(2026);
  }

  @Test
  @DisplayName("updateCourse는 강좌를 수정한 뒤 저장한다")
  void updateCourse() {
    Course course = course(1L);

    commandService.updateCourse(course, "자료구조", 2027, SemesterType.SPRING, StatusType.ACTIVE);

    assertThat(course.getName()).isEqualTo("자료구조");
    assertThat(course.getYear()).isEqualTo(2027);
    assertThat(course.getSemester()).isEqualTo(SemesterType.SPRING);
    assertThat(course.getStatus()).isEqualTo(StatusType.ACTIVE);
    verify(courseRepository).save(course);
  }

  @Test
  @DisplayName("deleteCourse는 삭제 시각을 기록한 뒤 저장한다 (soft delete)")
  void deleteCourse() {
    Course course = course(1L);

    commandService.deleteCourse(course);

    assertThat(course.getDeletedAt()).isNotNull();
    verify(courseRepository).save(course);
  }

  @Test
  @DisplayName("getAllCourses는 연도와 이름 순으로 정렬된 목록을 반환한다")
  void getAllCourses() {
    List<Course> courses = List.of(course(1L), course(2L));
    given(courseRepository.findAllOrderByYearAndName()).willReturn(courses);

    assertThat(queryService.getAllCourses()).isEqualTo(courses);
  }

  @Test
  @DisplayName("getCourseById는 강좌를 반환한다")
  void getCourseById() {
    Course course = course(1L);
    given(courseRepository.findById(1L)).willReturn(Optional.of(course));

    assertThat(queryService.getCourseById(1L)).isEqualTo(course);
  }

  @Test
  @DisplayName("getCourseById는 강좌가 없으면 CourseNotFoundException을 던진다")
  void getCourseByIdNotFound() {
    given(courseRepository.findById(404L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> queryService.getCourseById(404L))
        .isInstanceOf(CourseNotFoundException.class);
  }
}
