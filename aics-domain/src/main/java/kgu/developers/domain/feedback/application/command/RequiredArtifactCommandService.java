package kgu.developers.domain.feedback.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.feedback.application.query.RequiredArtifactQueryService;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactRepository;
import kgu.developers.domain.feedback.domain.RequiredArtifactType;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RequiredArtifactCommandService {
    private final RequiredArtifactRepository requiredArtifactRepository;
    private final RequiredArtifactQueryService requiredArtifactQueryService;
    private final MilestoneRepository milestoneRepository;

    public Long create(
            Long sectionId,
            Long milestoneId,
            RequiredArtifactType type,
            String label,
            boolean required,
            String allowedExtensions,
            Integer maxFileSizeMb
    ) {
        lockMilestone(sectionId, milestoneId);
        RequiredArtifact requiredArtifact = RequiredArtifact.create(
                milestoneId,
                type,
                label,
                required,
                allowedExtensions,
                maxFileSizeMb
        );
        return requiredArtifactRepository.save(requiredArtifact).getId();
    }

    public void update(
            Long sectionId,
            Long milestoneId,
            Long requiredArtifactId,
            RequiredArtifactType type,
            String label,
            boolean required,
            String allowedExtensions,
            Integer maxFileSizeMb
    ) {
        lockMilestone(sectionId, milestoneId);
        RequiredArtifact requiredArtifact = requiredArtifactQueryService.getRequiredArtifact(
                milestoneId,
                requiredArtifactId
        );
        requiredArtifact.update(type, label, required, allowedExtensions, maxFileSizeMb);
        requiredArtifactRepository.save(requiredArtifact);
    }

    public void delete(Long sectionId, Long milestoneId, Long requiredArtifactId) {
        lockMilestone(sectionId, milestoneId);
        RequiredArtifact requiredArtifact = requiredArtifactQueryService.getRequiredArtifact(
                milestoneId,
                requiredArtifactId
        );
        requiredArtifact.delete();
        requiredArtifactRepository.save(requiredArtifact);
    }

    // 삭제 중인 마일스톤과 같은 락을 잡아, 마일스톤 삭제와 동시에 진행되는 산출물 설정 저장이
    // 삭제된 마일스톤에 고아 데이터를 남기지 않도록 한다.
    private void lockMilestone(Long sectionId, Long milestoneId) {
        milestoneRepository.findByIdAndSectionIdForUpdate(milestoneId, sectionId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
    }
}
