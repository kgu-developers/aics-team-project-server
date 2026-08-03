package kgu.developers.domain.teammessage.application.query;

import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teammessage.domain.TeamMessageRepository;
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

    public Page<TeamMessage> getMessages(Long threadId, TeamMessageRelatedType relatedType, Pageable pageable) {
        if (relatedType != null) {
            return teamMessageRepository.findByThreadIdAndRelatedType(threadId, relatedType, pageable);
        }
        return teamMessageRepository.findByThreadId(threadId, pageable);
    }

    public long countUnread(Long threadId) {
        return teamMessageRepository.countByThreadIdAndIsReadFalse(threadId);
    }
}
