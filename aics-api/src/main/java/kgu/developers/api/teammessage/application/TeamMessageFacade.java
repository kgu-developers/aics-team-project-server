package kgu.developers.api.teammessage.application;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kgu.developers.api.team.application.TeamAccessValidator;
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
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.exception.UserNotFoundException;
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
    private final TeamAccessValidator teamAccessValidator;
    private final UserQueryService userQueryService;

    public TeamMessagePersistResponse postMessage(Long teamId, String senderId, TeamMessageCreateRequest request) {
        teamAccessValidator.validateMembershipOrProfessor(teamId, senderId);
        TeamThread teamThread = teamThreadCommandService.getOrCreateThread(teamId);
        TeamMessage teamMessage = teamMessageCommandService.postMessage(
            teamThread.getId(), senderId, request.relatedType(), request.relatedId(), request.message());
        String senderName = resolveSenderName(senderId);
        return TeamMessagePersistResponse.of(teamMessage, senderName);
    }

    public TeamMessagePageResponse getMessages(Long teamId, TeamMessageRelatedType relatedType, Pageable pageable, String userId) {
        teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
        TeamThread teamThread = teamThreadQueryService.getThread(teamId);
        Page<TeamMessage> messages = teamMessageQueryService.getMessages(teamThread.getId(), relatedType, pageable);
        List<Long> messageIds = messages.getContent().stream().map(TeamMessage::getId).toList();
        Set<Long> readMessageIds = teamMessageQueryService.findReadMessageIds(userId, messageIds);
        List<String> senderIds = messages.getContent().stream()
            .map(TeamMessage::getSenderId)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();
        Map<String, String> senderNamesBySenderId = userQueryService
            .getUsersByStudentNumbersIncludingDeleted(senderIds).stream()
            .collect(Collectors.toMap(User::getStudentNumber, User::getName));
        return TeamMessagePageResponse.from(messages, readMessageIds, senderNamesBySenderId);
    }

    private String resolveSenderName(String studentNumber) {
        if (studentNumber == null || studentNumber.isBlank()) {
            return null;
        }
        try {
            return userQueryService.getUserByStudentNumber(studentNumber).getName();
        } catch (UserNotFoundException e) {
            return null;
        }
    }

    public void updateImportant(Long messageId, boolean important, String userId) {
        Long teamId = resolveTeamId(messageId);
        teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
        teamMessageCommandService.updateImportant(messageId, important);
    }

    public void markAsRead(Long messageId, String userId) {
        Long teamId = resolveTeamId(messageId);
        teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
        teamMessageCommandService.markAsRead(messageId, userId);
    }

    public UnreadMessageCountResponse getUnreadCount(Long teamId, String userId) {
        teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
        TeamThread teamThread = teamThreadQueryService.getThread(teamId);
        long count = teamMessageQueryService.countUnread(teamThread.getId(), userId);
        return UnreadMessageCountResponse.of(count);
    }

    private Long resolveTeamId(Long messageId) {
        TeamMessage message = teamMessageQueryService.getMessage(messageId);
        return teamThreadQueryService.getThreadById(message.getThreadId()).getTeamId();
    }
}
