package kgu.developers.domain.team.domain;

import lombok.*;

import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class Team {
    private Long id;

    private Long sectionId;  // 분반 식별자

    private String name;  // 팀명
    private String kickoffRule;  // 팀 운영규칙
    private String meetingSchedule;  // 정기 회의일정
    private Status status;  // 상태

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static Team create(Long sectionId, String name, String kickoffRule, String meetingSchedule, Status status) {
        return Team.builder()
                .sectionId(sectionId)
                .name(name)
                .kickoffRule(kickoffRule)
                .meetingSchedule(meetingSchedule)
                .status(status)
                .build();
    }

    public void updateSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public void updateName(String name) {
        this.name = name;
    }


    public void updateKickoffRule(String kickoffRule) {
        this.kickoffRule = kickoffRule;
    }

    public void updateMeetingSchedule(String meetingSchedule) {
        this.meetingSchedule = meetingSchedule;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    public void delete() {
        deletedAt = LocalDateTime.now();
    }
}
