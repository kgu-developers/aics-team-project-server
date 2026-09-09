package kgu.developers.domain.feedback.application.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactRepository;
import kgu.developers.domain.feedback.exception.RequiredArtifactNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequiredArtifactQueryService {
    private final RequiredArtifactRepository requiredArtifactRepository;

    public List<RequiredArtifact> getRequiredArtifacts(Long milestoneId) {
        return requiredArtifactRepository.findAllByMilestoneId(milestoneId);
    }

    public RequiredArtifact getRequiredArtifact(Long milestoneId, Long requiredArtifactId) {
        return requiredArtifactRepository.findById(requiredArtifactId)
                .filter(requiredArtifact -> requiredArtifact.belongsToMilestone(milestoneId))
                .orElseThrow(RequiredArtifactNotFoundException::new);
    }
}
