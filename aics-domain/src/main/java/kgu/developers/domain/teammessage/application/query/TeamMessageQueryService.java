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
        return getMessages(threadId, relatedType, null, pageable);
    }

    public Page<TeamMessage> getMessages(Long threadId, TeamMessageRelatedType relatedType, Long relatedId, Pageable pageable) {
        if (relatedId != null && relatedType != null) {
            return teamMessageRepository.findByThreadIdAndRelatedTypeAndRelatedId(threadId, relatedType, relatedId, pageable);
        }
        if (relatedType != null) {
            return teamMessageRepository.findByThreadIdAndRelatedType(threadId, relatedType, pageable);
        }
        return teamMessageRepository.findByThreadId(threadId, pageable);
    }

    public Page<TeamMessage> getMessages(List<Long> threadIds, Pageable pageable) {
        return getMessages(threadIds, null, pageable);
    }

    public Page<TeamMessage> getMessages(List<Long> threadIds, TeamMessageRelatedType relatedType, Pageable pageable) {
        if (relatedType != null) {
            return teamMessageRepository.findByThreadIdInAndRelatedType(threadIds, relatedType, pageable);
        }
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

    public List<TeamMessage> getMessagesByBroadcastId(String broadcastId) {
        return teamMessageRepository.findAllByBroadcastId(broadcastId);
    }

    // 일괄 발송(broadcastId)으로 묶인 메시지는, 묶음을 그룹 단위로 보는 교수가 하나를 읽으면
    // 같은 묶음의 나머지도 전부 읽음 처리되어야 그룹 unreadCount와 실제 읽음 상태가 일치한다.
    // 반면 학생은 본인 팀에 온 메시지 1건만 읽는 것이지 다른 팀에 전송된 사본과는 무관하므로
    // 묶음 전체로 번지면 안 된다 — 호출자가 교수인 경우에만 묶음으로 확장한다.
    public List<Long> resolveMarkAsReadTargetIds(TeamMessage message, boolean cascadeBroadcastGroup) {
        if (!cascadeBroadcastGroup || message.getBroadcastId() == null) {
            return List.of(message.getId());
        }
        return getMessagesByBroadcastId(message.getBroadcastId()).stream()
            .map(TeamMessage::getId)
            .toList();
    }
}
