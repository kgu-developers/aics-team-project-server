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
        // 팀 행을 먼저 잠근 뒤 중복을 확인해야 동시 등록에서도 규칙이 지켜진다.
        // 소프트 삭제된 후보는 부분 유니크 인덱스에서 빠지므로 여기서도 활성 행만 본다.
        validateDuplicateTitle(teamId, title, null);
        if (topicCandidateRepository.existsByTeamIdAndProposerUserId(teamId, proposerUserId)) {
            throw new DuplicateTopicCandidateException();
        }

        return topicCandidateRepository.save(TopicCandidate.create(teamId, proposerUserId, title, description));
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
        topicCandidateRepository.findByTeamIdAndTitleForUpdate(teamId, title)
                .ifPresent(candidate -> {
                    if (excludeId == null || !candidate.getId().equals(excludeId)) {
                        throw new DuplicateTopicCandidateTitleException();
                    }
                });
    }
}