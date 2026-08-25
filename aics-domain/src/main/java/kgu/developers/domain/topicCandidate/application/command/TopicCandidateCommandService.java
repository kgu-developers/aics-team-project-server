package kgu.developers.domain.topicCandidate.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicCandidate.exception.DuplicateTopicCandidateException;
import kgu.developers.domain.topicCandidate.exception.DuplicateTopicCandidateTitleException;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicCandidateCommandService {
    private final TopicCandidateRepository topicCandidateRepository;

    public TopicCandidate createTopicCandidate(Long teamId, String proposerUserId, String title, String description) {
        if (topicCandidateRepository.existsByTeamIdAndProposerUserId(teamId, proposerUserId)) {
            throw new DuplicateTopicCandidateException();
        }

        TopicCandidate existing = topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(teamId, title)
                .orElse(null);
        if (existing != null) {
            if (existing.getDeletedAt() == null) {
                throw new DuplicateTopicCandidateTitleException();
            }
            existing.reactivate(proposerUserId, description);
            return topicCandidateRepository.save(existing);
        }

        TopicCandidate topicCandidate = TopicCandidate.create(teamId, proposerUserId, title, description);
        return topicCandidateRepository.save(topicCandidate);
    }

    public void updateTopicCandidate(Long id, String title, String description) {
        TopicCandidate topicCandidate = topicCandidateRepository.findByIdForUpdate(id)
                .orElseThrow(TopicCandidateNotFoundException::new);

        validateDuplicateTitle(topicCandidate.getTeamId(),
                               title != null ? title : topicCandidate.getTitle(),
                               id);

        if (title != null) {
            topicCandidate.updateTitle(title);
        }
        if (description != null) {
            topicCandidate.updateDescription(description);
        }
        
        topicCandidateRepository.save(topicCandidate);
    }

    public void deleteTopicCandidate(Long id) {
        TopicCandidate topicCandidate = topicCandidateRepository.findByIdForUpdate(id)
                .orElseThrow(TopicCandidateNotFoundException::new);
        topicCandidate.delete();
        topicCandidateRepository.save(topicCandidate);
    }

    private void validateDuplicateTitle(Long teamId, String title, Long excludeId) {
        topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(teamId, title)
                .ifPresent(candidate -> {
                    if (excludeId == null || !candidate.getId().equals(excludeId)) {
                        throw new DuplicateTopicCandidateTitleException();
                    }
                });
    }
}