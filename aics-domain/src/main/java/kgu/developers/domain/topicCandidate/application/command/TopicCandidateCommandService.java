package kgu.developers.domain.topicCandidate.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicCandidate.exception.DuplicateTopicCandidateTitleException;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicCandidateCommandService {
    private final TopicCandidateRepository topicCandidateRepository;

    public Long createTopicCandidate(Long teamId, String proposerUserId, String title, String description) {
        TopicCandidate existing = topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(teamId, title)
                .orElse(null);
        if (existing != null) {
            if (existing.getDeletedAt() == null) {
                throw new DuplicateTopicCandidateTitleException();
            }
            existing.reactivate(proposerUserId, description);
            return topicCandidateRepository.save(existing).getId();
        }

        TopicCandidate topicCandidate = TopicCandidate.create(teamId, proposerUserId, title, description);
        return topicCandidateRepository.save(topicCandidate).getId();
    }

    public void updateTopicCandidate(Long id, Long teamId, String title, String description) {
        TopicCandidate topicCandidate = topicCandidateRepository.findById(id)
                .orElseThrow(TopicCandidateNotFoundException::new);
        
        validateDuplicateTitle(teamId != null ? teamId : topicCandidate.getTeamId(), 
                               title != null ? title : topicCandidate.getTitle(), 
                               id);
        
        if (teamId != null) {
            topicCandidate.updateTeamId(teamId);
        }
        if (title != null) {
            topicCandidate.updateTitle(title);
        }
        if (description != null) {
            topicCandidate.updateDescription(description);
        }
        
        topicCandidateRepository.save(topicCandidate);
    }

    public void deleteTopicCandidate(Long id) {
        topicCandidateRepository.deleteById(id);
    }

    private void validateDuplicateTitle(Long teamId, String title, Long excludeId) {
        topicCandidateRepository.findActiveByTeamIdAndTitleForUpdate(teamId, title)
                .ifPresent(candidate -> {
                    if (excludeId == null || !candidate.getId().equals(excludeId)) {
                        throw new DuplicateTopicCandidateTitleException();
                    }
                });
    }
}