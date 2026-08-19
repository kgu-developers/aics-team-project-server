package kgu.developers.domain.section.application.command;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.course.domain.CourseRepository;
import kgu.developers.domain.course.exception.CourseNotFoundException;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SectionCommandService {
    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public Long createSection(String professorId, Long courseId, String code, String name, String classTime,
                              Integer capacity, LocalDateTime contactVisibleFrom, LocalDateTime contactVisibleUntil) {
        requireCourse(courseId);
        requireProfessor(professorId);
        Section section = Section.create(professorId, courseId, code, name, classTime, capacity,
                contactVisibleFrom, contactVisibleUntil);
        return sectionRepository.save(section).getId();
    }

    public void updateSection(Section section, String professorId, Long courseId, String code, String name,
                             String classTime, Integer capacity, LocalDateTime contactVisibleFrom,
                             LocalDateTime contactVisibleUntil) {
        if (courseId != null) {
            requireCourse(courseId);
        }
        if (professorId != null) {
            requireProfessor(professorId);
        }
        if (professorId != null) {
            section.updateProfessorId(professorId);
        }
        if (courseId != null) {
            section.updateCourseId(courseId);
        }
        if (code != null) {
            section.updateCode(code);
        }
        if (name != null) {
            section.updateName(name);
        }
        if (classTime != null) {
            section.updateClassTime(classTime);
        }
        if (capacity != null) {
            section.updateCapacity(capacity);
        }
        if (contactVisibleFrom != null || contactVisibleUntil != null) {
            section.updateContactVisiblePeriod(
                    contactVisibleFrom != null ? contactVisibleFrom : section.getContactVisibleFrom(),
                    contactVisibleUntil != null ? contactVisibleUntil : section.getContactVisibleUntil());
        }
        sectionRepository.save(section);
    }

    public void changeContactVisiblePeriod(Section section, LocalDateTime contactVisibleFrom,
                                           LocalDateTime contactVisibleUntil) {
        section.updateContactVisiblePeriod(contactVisibleFrom, contactVisibleUntil);
        sectionRepository.save(section);
    }

    public void changeCourse(Section section, Long courseId) {
        requireCourse(courseId);
        section.updateCourseId(courseId);
        sectionRepository.save(section);
    }

    public void changeProfessor(Section section, String professorId) {
        requireProfessor(professorId);
        section.updateProfessorId(professorId);
        sectionRepository.save(section);
    }

    public void deleteSection(Section section) {
        section.delete();
        sectionRepository.save(section);
    }

    private void requireCourse(Long courseId) {
        if (courseRepository.findById(courseId).isEmpty()) {
            throw new CourseNotFoundException();
        }
    }

    private void requireProfessor(String professorId) {
        if (userRepository.findByStudentNumber(professorId).isEmpty()) {
            throw new UserNotFoundException();
        }
    }
}
