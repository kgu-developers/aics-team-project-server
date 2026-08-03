package kgu.developers.domain.teammessage.application.command;

import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teammessage.domain.TeamMessageRepository;
import kgu.developers.domain.teammessage.exception.TeamMessageNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamMessageCommandService {

    private final TeamMessageRepository teamMessageRepository;

    public TeamMessage postMessage(Long threadId, String senderId, TeamMessageRelatedType relatedType,
                                    Long relatedId, String message) {
        TeamMessage teamMessage = TeamMessage.create(threadId, senderId, relatedType, relatedId, message);
        // TODO: Review(B1)가 구현되면 REVIEW 유형 메시지를 리뷰 등록 시 자동으로 게시하는 훅을 여기에 연결한다.
        // TODO: Notification 도메인이 구현되면 QUESTION 유형 메시지 등록 시 담당 교수에게 알림을 발송하는 훅을 여기에 연결한다.
        return teamMessageRepository.save(teamMessage);
    }

    public TeamMessage updateImportant(Long messageId, boolean important) {
        TeamMessage teamMessage = teamMessageRepository.findById(messageId)
            .orElseThrow(TeamMessageNotFoundException::new);
        teamMessage.updateImportant(important);
        return teamMessageRepository.save(teamMessage);
    }

    public TeamMessage markAsRead(Long messageId) {
        TeamMessage teamMessage = teamMessageRepository.findById(messageId)
            .orElseThrow(TeamMessageNotFoundException::new);
        teamMessage.markAsRead();
        return teamMessageRepository.save(teamMessage);
    }
}
