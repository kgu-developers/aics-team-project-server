package kgu.developers.domain.teamthread.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TeamThread {

    private Long id;
    private Long teamId;
    private LocalDateTime createdAt;

    public static TeamThread create(Long teamId) {
        return TeamThread.builder()
            .teamId(teamId)
            .build();
    }
}
