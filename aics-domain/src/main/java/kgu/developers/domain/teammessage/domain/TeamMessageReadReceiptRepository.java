package kgu.developers.domain.teammessage.domain;

import java.util.List;
import java.util.Set;

public interface TeamMessageReadReceiptRepository {

    TeamMessageReadReceipt save(TeamMessageReadReceipt readReceipt);

    boolean existsByMessageIdAndUserId(Long messageId, String userId);

    Set<Long> findReadMessageIds(String userId, List<Long> messageIds);
}
