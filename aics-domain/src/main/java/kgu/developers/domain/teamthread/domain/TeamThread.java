package kgu.developers.domain.teamthread.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 팀-교수 커뮤니케이션 히스토리(피드백/질문답변/리뷰 로그)를 담는 대화방.
 * 팀당 정확히 하나만 존재한다 (team_id UNIQUE).
 */
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
