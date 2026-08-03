package kgu.developers.api.teammessage.application;

import kgu.developers.api.teammessage.presentation.request.TeamMessageCreateRequest;
import kgu.developers.api.teammessage.presentation.response.TeamMessagePageResponse;
import kgu.developers.api.teammessage.presentation.response.TeamMessagePersistResponse;
import kgu.developers.api.teammessage.presentation.response.UnreadMessageCountResponse;
import kgu.developers.domain.teammessage.application.command.TeamMessageCommandService;
import kgu.developers.domain.teammessage.application.query.TeamMessageQueryService;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teamthread.application.command.TeamThreadCommandService;
import kgu.developers.domain.teamthread.application.query.TeamThreadQueryService;
import kgu.developers.domain.teamthread.domain.TeamThread;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class TeamMessageFacade {

    private final TeamThreadCommandService teamThreadCommandService;
    private final TeamThreadQueryService teamThreadQueryService;
    private final TeamMessageCommandService teamMessageCommandService;
    private final TeamMessageQueryService teamMessageQueryService;

    public TeamMessagePersistResponse postMessage(Long teamId, TeamMessageCreateRequest request) {
        TeamThread teamThread = teamThreadCommandService.getOrCreateThread(teamId);
        TeamMessage teamMessage = teamMessageCommandService.postMessage(
            teamThread.getId(), request.senderId(), request.relatedType(), request.relatedId(), request.message());
        return TeamMessagePersistResponse.of(teamMessage);
    }

    public TeamMessagePageResponse getMessages(Long teamId, TeamMessageRelatedType relatedType, Pageable pageable) {
        TeamThread teamThread = teamThreadQueryService.getThread(teamId);
        Page<TeamMessage> messages = teamMessageQueryService.getMessages(teamThread.getId(), relatedType, pageable);
        return TeamMessagePageResponse.from(messages);
    }

    public void updateImportant(Long messageId, boolean important) {
        teamMessageCommandService.updateImportant(messageId, important);
    }

    public void markAsRead(Long messageId) {
        teamMessageCommandService.markAsRead(messageId);
    }

    public UnreadMessageCountResponse getUnreadCount(Long teamId) {
        TeamThread teamThread = teamThreadQueryService.getThread(teamId);
        long count = teamMessageQueryService.countUnread(teamThread.getId());
        return UnreadMessageCountResponse.of(count);
    }
}
