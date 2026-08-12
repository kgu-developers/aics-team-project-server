package kgu.developers.domain.section.application.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.course.domain.CourseRepository;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.exception.CourseNotFoundException;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
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

    public SectionDetail getSectionById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(SectionNotFoundException::new);
    }

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

    /**
     * 강좌 조건으로 걸러낸 담당 분반 목록. null인 조건은 무시한다.
     * 한 교수의 분반은 많아야 수십 건이라 조회 후 메모리에서 거른다.
     */
    public List<SectionDetail> getSectionsByProfessorId(String professorId, StatusType status, Integer year,
                                                        SemesterType semester) {
        return getSectionsByProfessorId(professorId).stream()
                .filter(detail -> status == null || detail.course().getStatus() == status)
                .filter(detail -> year == null || detail.course().getYear() == year)
                .filter(detail -> semester == null || detail.course().getSemester() == semester)
                .toList();
    }
}
