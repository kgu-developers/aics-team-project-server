package kgu.developers.admin.milestone.application;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import kgu.developers.domain.section.application.query.SectionQueryService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MilestoneAccessValidator {
    private final SectionQueryService sectionQueryService;

    public void validateSectionAccess(Long sectionId, String professorId) {
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
            throw new AccessDeniedException("담당 교수만 분반 마일스톤에 접근할 수 있습니다.");
        }
    }
}
