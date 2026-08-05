package kgu.developers.admin.course.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.admin.course.application.CourseFacade;
import kgu.developers.admin.course.presentation.request.CourseRequest;
import kgu.developers.admin.course.presentation.response.CourseListResponse;
import kgu.developers.admin.course.presentation.response.CoursePersistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop/courses")
public class CourseControllerImpl implements CourseController {

  private final CourseFacade courseFacade;

  @Override
  @PostMapping
  public ResponseEntity<CoursePersistResponse> createCourse(
      @Valid @RequestBody CourseRequest request) {
    CoursePersistResponse response = courseFacade.createCourse(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{id}")
  @Override
  public ResponseEntity<Course> getCourseById(
      @Positive @PathVariable Long id) {
    Course response = courseFacade.getCourseById(id);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping
  public ResponseEntity<CourseListResponse> getCourses() {
    CourseListResponse response = courseFacade.getAllCourses();
    return ResponseEntity.ok(response);
  }

  @Override
  @PutMapping("/{id}")
  public ResponseEntity<Void> updateCourse(
      @Positive @PathVariable Long id,
      @Valid @RequestBody CourseRequest request) {
    courseFacade.updateCourse(id, request);
    return ResponseEntity.noContent().build();
  }

  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCourse(
      @Positive @PathVariable Long id) {
    courseFacade.deleteCourse(id);
    return ResponseEntity.noContent().build();
  }
}
