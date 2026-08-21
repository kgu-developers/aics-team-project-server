package kgu.developers.admin.importcommon;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * 명단 업로드는 관리자 API에 있지만 조교도 써야 하므로, 경로 자체는 ROLE_ADMIN을 요구하지 않는다.
 * 대신 여기서 전역 관리자이거나 그 분반의 조교·담당 교수인지 확인해, 남의 분반 명단을 건드리지 못하게 한다.
 */
@Component
@RequiredArgsConstructor
public class SectionStaffValidator {

    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;

    public void validate(Long sectionId, String studentNumber) {
        if (isAdmin(studentNumber) || isAssistant(sectionId, studentNumber)
            || sectionRepository.existsActiveByIdAndProfessorId(sectionId, studentNumber)) {
            return;
        }
        throw new AccessDeniedException("해당 분반의 조교 또는 담당 교수만 명단을 업로드할 수 있습니다.");
    }

    private boolean isAdmin(String studentNumber) {
        return userRepository.findByStudentNumber(studentNumber)
            .map(User::getGlobalRole)
            .filter(role -> role == UserGlobalRole.ADMIN)
            .isPresent();
    }

    private boolean isAssistant(Long sectionId, String studentNumber) {
        return enrollmentRepository.findBySectionIdAndUserId(sectionId, studentNumber)
            .filter(enrollment -> enrollment.getRole() == Role.ASSISTANT)
            .isPresent();
    }
}
