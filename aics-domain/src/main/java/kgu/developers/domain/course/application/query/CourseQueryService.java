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

    /** 강좌는 교수가 소유하지 않는 공용 카탈로그라, 본인 분반이 하나라도 있는 강좌만 거른다. */
    public List<Course> getCoursesOwnedByProfessor(String professorId) {
        return courseRepository.findAllOwnedByProfessor(professorId);
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(CourseNotFoundException::new);
    }
}

