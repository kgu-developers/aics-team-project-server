package kgu.developers.domain.feedback.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.feedback.application.query.RequiredArtifactQueryService;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactRepository;
import kgu.developers.domain.feedback.domain.RequiredArtifactType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RequiredArtifactCommandService {
    private final RequiredArtifactRepository requiredArtifactRepository;
    private final RequiredArtifactQueryService requiredArtifactQueryService;

    public Long create(
            Long milestoneId,
            RequiredArtifactType type,
            String label,
            boolean required,
            String allowedExtensions,
            Integer maxFileSizeMb
    ) {
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
            Long milestoneId,
            Long requiredArtifactId,
            RequiredArtifactType type,
            String label,
            boolean required,
            String allowedExtensions,
            Integer maxFileSizeMb
    ) {
        RequiredArtifact requiredArtifact = requiredArtifactQueryService.getRequiredArtifact(
                milestoneId,
                requiredArtifactId
        );
        requiredArtifact.update(type, label, required, allowedExtensions, maxFileSizeMb);
        requiredArtifactRepository.save(requiredArtifact);
    }

    public void delete(Long milestoneId, Long requiredArtifactId) {
        RequiredArtifact requiredArtifact = requiredArtifactQueryService.getRequiredArtifact(
                milestoneId,
                requiredArtifactId
        );
        requiredArtifact.delete();
        requiredArtifactRepository.save(requiredArtifact);
    }
}
