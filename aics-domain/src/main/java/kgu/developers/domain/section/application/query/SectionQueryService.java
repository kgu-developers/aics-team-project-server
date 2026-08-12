package kgu.developers.domain.section.application.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.course.domain.CourseRepository;
import kgu.developers.domain.course.exception.CourseNotFoundException;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SectionQueryService {
    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public List<SectionDetail> getSectionsByCourseId(Long courseId) {
        if (courseRepository.findById(courseId).isEmpty()) {
            throw new CourseNotFoundException();
        }
        return sectionRepository.findAllByCourseId(courseId);
    }

    public List<SectionDetail> getSectionsByProfessorId(String professorId) {
        if (userRepository.findByStudentNumber(professorId).isEmpty()) {
            throw new UserNotFoundException();
        }
        return sectionRepository.findAllByProfessorId(professorId);
    }
}
