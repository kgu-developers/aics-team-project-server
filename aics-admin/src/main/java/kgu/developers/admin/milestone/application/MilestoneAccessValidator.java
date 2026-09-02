package kgu.developers.admin.milestone.application;

import org.springframework.stereotype.Component;

import kgu.developers.domain.milestone.exception.MilestoneSectionAccessDeniedException;
import kgu.developers.domain.section.application.query.SectionQueryService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MilestoneAccessValidator {
    private final SectionQueryService sectionQueryService;

    public void validateSectionAccess(Long sectionId, String professorId) {
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
            throw new MilestoneSectionAccessDeniedException();
        }
    }
}
