package kgu.developers.domain.course.application.query;

import java.util.List;

import kgu.developers.domain.course.exception.CourseNotFoundException;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService {
    private final CourseRepository courseRepository;

    public List<Course> getAllCourses() {
        return courseRepository.findAllOrderByYearAndName();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(CourseNotFoundException::new);
    }
}

