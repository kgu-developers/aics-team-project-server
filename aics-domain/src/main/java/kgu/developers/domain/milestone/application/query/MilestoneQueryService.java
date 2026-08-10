package kgu.developers.domain.milestone.application.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.milestone.exception.MilestoneSectionMismatchException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MilestoneQueryService {
    private final MilestoneRepository milestoneRepository;

    public List<Milestone> getMilestones(Long sectionId, MilestoneStatus status) {
        validateSectionId(sectionId);
        if (status == null) {
            return milestoneRepository.findAllBySectionIdOrderByWeekNumber(sectionId);
        }
        return milestoneRepository.findAllBySectionIdAndStatusOrderByWeekNumber(sectionId, status);
    }

    public Milestone getMilestone(Long sectionId, Long milestoneId) {
        validateSectionId(sectionId);
        Milestone milestone = getRequiredMilestone(milestoneId);
        if (!milestone.belongsToSection(sectionId)) {
            throw new MilestoneSectionMismatchException(milestoneId, sectionId);
        }
        return milestone;
    }

    private Milestone getRequiredMilestone(Long milestoneId) {
        validateMilestoneId(milestoneId);
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
    }

    private void validateMilestoneId(Long milestoneId) {
        if (milestoneId == null || milestoneId <= 0) {
            throw new IllegalArgumentException("마일스톤 식별자는 양수여야 합니다.");
        }
    }

    private void validateSectionId(Long sectionId) {
        if (sectionId == null || sectionId <= 0) {
            throw new IllegalArgumentException("분반 식별자는 양수여야 합니다.");
        }
    }
}
