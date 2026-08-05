package kgu.developers.domain.course.application.command;

import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.CourseRepository;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseCommandService {
    private final CourseRepository courseRepository;

    public Long createCourse(String name, int year, SemesterType semester, StatusType status) {
        Course course = Course.create(name, year, semester, status);
        return courseRepository.save(course).getId();
    }

    public void updateCourse(Course course, String name, int year, SemesterType semester, StatusType status) {
        course.updateName(name);
        course.updateYear(year);
        course.updateSemester(semester);
        course.updateStatus(status);
        courseRepository.save(course);
    }

    public void deleteCourse(Course course) {
        course.delete();
        courseRepository.save(course);
    }
}
