package mock.repository;

import java.util.List;
import java.util.Set;

import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageUnreadRepository;

public class FakeTeamMessageUnreadRepository implements TeamMessageUnreadRepository {

    private final FakeTeamMessageRepository teamMessageRepository;
    private final FakeTeamMessageReadReceiptRepository readReceiptRepository;

    public FakeTeamMessageUnreadRepository(
        FakeTeamMessageRepository teamMessageRepository,
        FakeTeamMessageReadReceiptRepository readReceiptRepository
    ) {
        this.teamMessageRepository = teamMessageRepository;
        this.readReceiptRepository = readReceiptRepository;
    }

    @Override
    public long countUnreadByThreadIdIn(List<Long> threadIds, String userId) {
        List<TeamMessage> messages = teamMessageRepository.findMessagesByThreadIdIn(threadIds);
        List<Long> messageIds = messages.stream().map(TeamMessage::getId).toList();
        Set<Long> readMessageIds = readReceiptRepository.findReadMessageIds(userId, messageIds);
        // broadcastId가 같은 메시지는 markAsRead가 항상 묶어서 처리하므로 한 그룹은 항상 전부읽음/전부안읽음이다.
        // 그래서 안읽음 메시지의 그룹 수를 세면 그게 곧 "안읽은 묶음 수"가 된다.
        return messages.stream()
            .filter(message -> !readMessageIds.contains(message.getId()))
            .map(message -> message.getBroadcastId() != null ? message.getBroadcastId() : "msg-" + message.getId())
            .distinct()
            .count();
    }
}
