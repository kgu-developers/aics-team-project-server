package kgu.developers.domain.topicCandidate.application.command;

import java.util.Arrays;

import org.springframework.dao.OptimisticLockingFailureException;
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
        Long currentTeamId = topicCandidateRepository.findById(id)
                .map(TopicCandidate::getTeamId)
                .orElseThrow(TopicCandidateNotFoundException::new);
        lockTeamsInIdOrder(currentTeamId, teamId != null ? teamId : currentTeamId);

        TopicCandidate topicCandidate = topicCandidateRepository.findByIdForUpdate(id)
                .orElseThrow(TopicCandidateNotFoundException::new);
        if (!topicCandidate.getTeamId().equals(currentTeamId)) {
            throw new OptimisticLockingFailureException(
                    "주제 후보 %d의 팀이 잠금 획득 전에 변경되었습니다".formatted(id));
        }

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
        TopicCandidate topicCandidate = topicCandidateRepository.findByIdForUpdate(id)
                .orElseThrow(TopicCandidateNotFoundException::new);
        topicCandidate.delete();
        topicCandidateRepository.save(topicCandidate);
    }

    private void lockTeamsInIdOrder(Long... teamIds) {
        Arrays.stream(teamIds)
                .distinct()
                .sorted()
                .forEach(topicCandidateRepository::lockTeamForUpdate);
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