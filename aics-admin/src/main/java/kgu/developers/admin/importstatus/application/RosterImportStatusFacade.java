package kgu.developers.admin.importstatus.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.admin.importcommon.SectionStaffValidator;
import kgu.developers.admin.importstatus.presentation.response.RosterImportAppliedResponse;
import kgu.developers.admin.importstatus.presentation.response.RosterImportStatusResponse;
import kgu.developers.domain.importBatch.domain.ImportBatchRepository;
import kgu.developers.domain.importBatch.domain.Type;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RosterImportStatusFacade {

    private final ImportBatchRepository importBatchRepository;
    private final SectionRepository sectionRepository;
    private final SectionStaffValidator sectionStaffValidator;

    public RosterImportStatusResponse getStatus(Long sectionId, String userId) {
        sectionStaffValidator.validate(sectionId, userId);
        if (sectionRepository.findById(sectionId).isEmpty()) {
            throw new SectionNotFoundException();
        }

        RosterImportAppliedResponse studentRoster = importBatchRepository
            .findLatestApplied(sectionId, Type.ENROLLMENT)
            .map(RosterImportAppliedResponse::from)
            .orElse(null);
        RosterImportAppliedResponse teamRoster = importBatchRepository
            .findLatestApplied(sectionId, Type.TEAM)
            .map(RosterImportAppliedResponse::from)
            .orElse(null);
        return new RosterImportStatusResponse(studentRoster, teamRoster);
    }
}
