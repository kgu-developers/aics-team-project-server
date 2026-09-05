package kgu.developers.domain.teammessage.application.query;

import java.util.List;
import java.util.Set;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageReadReceiptRepository;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teammessage.domain.TeamMessageRepository;
import kgu.developers.domain.teammessage.domain.TeamMessageUnreadRepository;
import kgu.developers.domain.teammessage.exception.TeamMessageNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TeamMessageQueryService {

    private final TeamMessageRepository teamMessageRepository;
    private final TeamMessageReadReceiptRepository teamMessageReadReceiptRepository;
    private final TeamMessageUnreadRepository teamMessageUnreadRepository;

    public TeamMessage getMessage(Long id) {
        return teamMessageRepository.findById(id)
            .orElseThrow(TeamMessageNotFoundException::new);
    }

    public Page<TeamMessage> getMessages(Long threadId, TeamMessageRelatedType relatedType, Pageable pageable) {
        if (relatedType != null) {
            return teamMessageRepository.findByThreadIdAndRelatedType(threadId, relatedType, pageable);
        }
        return teamMessageRepository.findByThreadId(threadId, pageable);
    }

    public Page<TeamMessage> getMessages(List<Long> threadIds, Pageable pageable) {
        return teamMessageRepository.findByThreadIdIn(threadIds, pageable);
    }

    public Set<Long> findReadMessageIds(String userId, List<Long> messageIds) {
        return teamMessageReadReceiptRepository.findReadMessageIds(userId, messageIds);
    }

    public long countUnread(Long threadId, String userId) {
        return teamMessageUnreadRepository.countUnreadByThreadIdIn(List.of(threadId), userId);
    }

    public long countUnread(List<Long> threadIds, String userId) {
        return teamMessageUnreadRepository.countUnreadByThreadIdIn(threadIds, userId);
    }
}
