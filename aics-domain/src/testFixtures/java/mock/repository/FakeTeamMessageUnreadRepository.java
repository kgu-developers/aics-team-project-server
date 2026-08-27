package mock.repository;

import java.util.List;
import java.util.Set;

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
        List<Long> messageIds = teamMessageRepository.findIdsByThreadIdIn(threadIds);
        Set<Long> readMessageIds = readReceiptRepository.findReadMessageIds(userId, messageIds);
        return messageIds.size() - readMessageIds.size();
    }
}
