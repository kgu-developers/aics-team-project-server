package kgu.developers.domain.section.application.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.course.domain.CourseRepository;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.exception.CourseNotFoundException;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Status;
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
    private final EnrollmentRepository enrollmentRepository;

    public SectionDetail getSectionById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(SectionNotFoundException::new);
    }

    /** 마일스톤 등 외부 연동에서 sectionId를 다루기 전에 소유 교수인지 확인하는 용도 */
    public boolean isActiveSectionOwnedByProfessor(Long sectionId, String professorId) {
        return sectionRepository.existsActiveByIdAndProfessorId(sectionId, professorId);
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

    public List<SectionDetail> getSectionsByProfessorId(String professorId, StatusType status, Integer year,
                                                        SemesterType semester) {
        return filter(getSectionsByProfessorId(professorId), status, year, semester);
    }

    /** 학생이 수강 중인 분반. 교수 소유가 아니라 수강 정보(Enrollment)를 거쳐 찾는다. */
    public List<SectionDetail> getSectionsByStudentNumber(String studentNumber) {
        if (userRepository.findByStudentNumber(studentNumber).isEmpty()) {
            throw new UserNotFoundException();
        }
        List<Long> sectionIds = enrollmentRepository.findAllByUserId(studentNumber).stream()
                .filter(enrollment -> enrollment.getStatus() == Status.ACTIVE)
                .map(Enrollment::getSectionId)
                .toList();
        return sectionRepository.findAllByIdIn(sectionIds);
    }

    public List<SectionDetail> getSectionsByStudentNumber(String studentNumber, StatusType status, Integer year,
                                                          SemesterType semester) {
        return filter(getSectionsByStudentNumber(studentNumber), status, year, semester);
    }

    private List<SectionDetail> filter(List<SectionDetail> details, StatusType status, Integer year,
                                       SemesterType semester) {
        return details.stream()
                .filter(detail -> status == null || detail.course().getStatus() == status)
                .filter(detail -> year == null || year.equals(detail.course().getYear()))
                .filter(detail -> semester == null || detail.course().getSemester() == semester)
                .toList();
    }
}
