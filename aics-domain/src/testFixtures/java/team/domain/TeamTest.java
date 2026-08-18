package team.domain;

import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeamTest {

    @Test
    @DisplayName("Team.create 로 정적 팩토리 생성 시 올바르게 객체가 생성된다")
    void createTeam() {
        Team team = Team.create(1L, "알고리즘 A팀", "주 1회 코드리뷰", "매주 화요일 19시", Status.FORMING);

        assertThat(team.getSectionId()).isEqualTo(1L);
        assertThat(team.getName()).isEqualTo("알고리즘 A팀");
        assertThat(team.getKickoffRule()).isEqualTo("주 1회 코드리뷰");
        assertThat(team.getMeetingSchedule()).isEqualTo("매주 화요일 19시");
        assertThat(team.getStatus()).isEqualTo(Status.FORMING);
    }

    @Test
    @DisplayName("팀명, 운영규칙, 회의일정, 상태 정보를 수정할 수 있다")
    void updateTeamInfo() {
        Team team = Team.create(1L, "알고리즘 A팀", "주 1회 코드리뷰", "매주 화요일 19시", Status.FORMING);

        team.updateName("알고리즘 B팀");
        team.updateKickoffRule("주 2회 모임");
        team.updateMeetingSchedule("매주 목요일 18시");
        team.updateStatus(Status.CONFIRMED);

        assertThat(team.getName()).isEqualTo("알고리즘 B팀");
        assertThat(team.getKickoffRule()).isEqualTo("주 2회 모임");
        assertThat(team.getMeetingSchedule()).isEqualTo("매주 목요일 18시");
        assertThat(team.getStatus()).isEqualTo(Status.CONFIRMED);
    }

    @Test
    @DisplayName("delete 호출 시 deletedAt 필드에 현재 시각이 기록된다")
    void deleteTeam() {
        Team team = Team.create(1L, "알고리즘 A팀", "주 1회 코드리뷰", "매주 화요일 19시", Status.FORMING);

        assertThat(team.getDeletedAt()).isNull();
        team.delete();
        assertThat(team.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Status enum 의 설명이 정상적으로 출력된다")
    void statusDescription() {
        assertThat(Status.FORMING.getDescription()).isEqualTo("형성중");
        assertThat(Status.CONFIRMED.getDescription()).isEqualTo("확립");
    }
}
