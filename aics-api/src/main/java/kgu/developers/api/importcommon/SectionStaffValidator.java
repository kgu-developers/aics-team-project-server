package kgu.developers.api.importcommon;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.section.domain.SectionRepository;
import lombok.RequiredArgsConstructor;

/** 명단 업로드처럼 분반 운영 권한이 필요한 작업에서, 해당 분반의 조교나 담당 교수인지 확인한다. */
@Component
@RequiredArgsConstructor
public class SectionStaffValidator {

    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;

    public void validate(Long sectionId, String studentNumber) {
        boolean assistant = enrollmentRepository.findBySectionIdAndUserId(sectionId, studentNumber)
            .filter(enrollment -> enrollment.getRole() == Role.ASSISTANT)
            .isPresent();
        if (assistant || sectionRepository.existsActiveByIdAndProfessorId(sectionId, studentNumber)) {
            return;
        }
        throw new AccessDeniedException("해당 분반의 조교 또는 담당 교수만 명단을 업로드할 수 있습니다.");
    }
}
