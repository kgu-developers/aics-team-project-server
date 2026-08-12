package kgu.developers.domain.teammessage.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TeamMessageReadReceipt {

    private Long id;
    private Long messageId;
    private String userId;

    public static TeamMessageReadReceipt create(Long messageId, String userId) {
        return TeamMessageReadReceipt.builder()
            .messageId(messageId)
            .userId(userId)
            .build();
    }
}
