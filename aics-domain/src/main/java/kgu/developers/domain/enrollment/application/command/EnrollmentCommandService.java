package kgu.developers.domain.enrollment.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.enrollment.exception.DuplicateEnrollmentException;
import kgu.developers.domain.enrollment.exception.EnrollmentNotFoundException;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentCommandService {
  private final EnrollmentRepository enrollmentRepository;
  private final SectionRepository sectionRepository;
  private final UserRepository userRepository;

  public Long createEnrollment(Long sectionId, String studentNumber, Role role) {
    if (sectionRepository.findById(sectionId).isEmpty()) {
      throw new SectionNotFoundException();
    }
    if (userRepository.findByStudentNumber(studentNumber).isEmpty()) {
      throw new UserNotFoundException();
    }

    Enrollment existing = enrollmentRepository.findIncludingDeleted(sectionId, studentNumber).orElse(null);
    if (existing != null) {
      if (existing.getDeletedAt() == null) {
        throw new DuplicateEnrollmentException();
      }
      existing.reactivate(role);
      return enrollmentRepository.save(existing).getId();
    }

    Enrollment enrollment = Enrollment.create(sectionId, studentNumber, role, Status.ACTIVE);
    return enrollmentRepository.save(enrollment).getId();
  }

  public void updateEnrollment(Long sectionId, String studentNumber, Role role, Status status) {
    Enrollment enrollment = enrollmentRepository.findBySectionIdAndUserId(sectionId, studentNumber)
        .orElseThrow(EnrollmentNotFoundException::new);

    if (role != null) {
      enrollment.updateRole(role);
    }
    if (status != null) {
      enrollment.updateStatus(status);
    }
    enrollmentRepository.save(enrollment);
  }
}
