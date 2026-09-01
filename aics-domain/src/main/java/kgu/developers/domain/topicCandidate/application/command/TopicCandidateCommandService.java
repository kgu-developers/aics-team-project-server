package kgu.developers.domain.topicCandidate.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicCandidate.exception.DuplicateTopicCandidateException;
import kgu.developers.domain.topicCandidate.exception.DuplicateTopicCandidateTitleException;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicCandidateCommandService {
    private final TopicCandidateRepository topicCandidateRepository;

    public TopicCandidate createTopicCandidate(Long teamId, String proposerUserId, String title, String description) {
        // 팀 행을 먼저 잠근 뒤 중복을 확인해야 동시 등록에서도 규칙이 지켜진다.
        TopicCandidate existing = topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(teamId, title)
                .orElse(null);
        if (topicCandidateRepository.existsByTeamIdAndProposerUserId(teamId, proposerUserId)) {
            throw new DuplicateTopicCandidateException();
        }

        if (existing != null) {
            if (existing.getDeletedAt() == null) {
                throw new DuplicateTopicCandidateTitleException();
            }
            existing.reactivate(proposerUserId, description);
            return saveTranslatingDuplicate(existing);
        }

        return saveTranslatingDuplicate(TopicCandidate.create(teamId, proposerUserId, title, description));
    }

    // 소프트 삭제된 행도 (team_id, proposer_user_id) 유니크 제약을 차지하므로,
    // 조회로 걸러지지 않는 충돌은 제약 위반을 도메인 예외로 바꿔 응답한다.
    private TopicCandidate saveTranslatingDuplicate(TopicCandidate topicCandidate) {
        try {
            return topicCandidateRepository.save(topicCandidate);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueConstraintViolation(e, "uk_topic_candidate_team_proposer")) {
                throw new DuplicateTopicCandidateException();
            }
            throw e;
        }
    }

    private boolean isUniqueConstraintViolation(DataIntegrityViolationException e, String constraintName) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(constraintName);
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