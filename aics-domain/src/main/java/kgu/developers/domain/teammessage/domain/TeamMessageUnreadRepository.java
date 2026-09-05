package kgu.developers.domain.teammessage.domain;

import java.util.List;

public interface TeamMessageUnreadRepository {

    long countUnreadByThreadIdIn(List<Long> threadIds, String userId);
}
