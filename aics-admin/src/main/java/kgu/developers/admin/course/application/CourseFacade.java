package kgu.developers.admin.course.application;

import kgu.developers.domain.course.application.command.CourseCommandService;
import kgu.developers.domain.course.application.query.CourseQueryService;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.admin.course.presentation.request.CourseRequest;
import kgu.developers.admin.course.presentation.response.CourseListResponse;
import kgu.developers.admin.course.presentation.response.CoursePersistResponse;
import kgu.developers.admin.course.presentation.response.CourseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseFacade {
	private final CourseCommandService courseCommandService;
	private final CourseQueryService courseQueryService;

	public CoursePersistResponse createCourse(CourseRequest request) {
		Long id = courseCommandService.createCourse(request.name(), request.year(), request.semester(), request.status());
		return CoursePersistResponse.of(id);
	}

	public void updateCourse(Long id, CourseRequest request) {
		Course course = courseQueryService.getCourseById(id);
		courseCommandService.updateCourse(course, request.name(), request.year(), request.semester(), request.status());
	}

	public void deleteCourse(Long id) {
		Course course = courseQueryService.getCourseById(id);
		courseCommandService.deleteCourse(course);
	}

	public CourseResponse getCourseById(Long id) {
        return CourseResponse.from(courseQueryService.getCourseById(id));
	}

	public CourseListResponse getAllCourses() {
		return CourseListResponse.from(courseQueryService.getAllCourses());
	}
}
