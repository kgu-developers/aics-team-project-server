package teamMember.domain;

import kgu.developers.domain.teamMember.domain.TeamMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeamMemberTest {

    @Test
    @DisplayName("TeamMember.create 로 정적 팩토리 생성 시 teamId, userId, isLeader, projectRole 이 올바르게 설정된다")
    void createTeamMember() {
        TeamMember teamMember = TeamMember.create(1L, "202012345", true, "백엔드 및 PM");

        assertThat(teamMember.getTeamId()).isEqualTo(1L);
        assertThat(teamMember.getUserId()).isEqualTo("202012345");
        assertThat(teamMember.isLeader()).isTrue();
        assertThat(teamMember.getProjectRole()).isEqualTo("백엔드 및 PM");
    }

    @Test
    @DisplayName("팀장 여부(isLeader) 및 프로젝트 역할(projectRole)을 수정할 수 있다")
    void updateRoleAndLeaderStatus() {
        TeamMember teamMember = TeamMember.create(1L, "202012345", false, "프론트엔드");

        teamMember.updateIsLeader(true);
        teamMember.updateProjectRole("풀스택");

        assertThat(teamMember.isLeader()).isTrue();
        assertThat(teamMember.getProjectRole()).isEqualTo("풀스택");
    }

    @Test
    @DisplayName("delete 호출 시 deletedAt 필드에 삭제 시각이 저장된다")
    void deleteTeamMember() {
        TeamMember teamMember = TeamMember.create(1L, "202012345", false, "디자이너");

        assertThat(teamMember.getDeletedAt()).isNull();
        teamMember.delete();
        assertThat(teamMember.getDeletedAt()).isNotNull();
    }
}
