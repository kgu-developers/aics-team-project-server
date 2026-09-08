package kgu.developers.domain.auditLog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamMembersAuditSnapshotTest {

    @Test
    @DisplayName("역할분담이 같으면 true를 반환한다")
    void returnsTrueWhenProjectRolesAreEqual() {
        // given
        TeamMembersAuditSnapshot snapshot1 = new TeamMembersAuditSnapshot(List.of(
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210001", false, "개발"),
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210002", false, "디자인")
        ));
        TeamMembersAuditSnapshot snapshot2 = new TeamMembersAuditSnapshot(List.of(
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210001", false, "개발"),
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210002", false, "디자인")
        ));

        // when
        boolean result = snapshot1.projectRolesEqual(snapshot2);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("역할분담이 다르면 false를 반환한다")
    void returnsFalseWhenProjectRolesAreDifferent() {
        // given
        TeamMembersAuditSnapshot snapshot1 = new TeamMembersAuditSnapshot(List.of(
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210001", false, "개발"),
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210002", false, "디자인")
        ));
        TeamMembersAuditSnapshot snapshot2 = new TeamMembersAuditSnapshot(List.of(
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210001", false, "개발"),
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210002", false, "기획")
        ));

        // when
        boolean result = snapshot1.projectRolesEqual(snapshot2);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("순서가 다르지만 내용이 같으면 true를 반환한다")
    void returnsTrueWhenOrderIsDifferentButContentIsSame() {
        // given
        TeamMembersAuditSnapshot snapshot1 = new TeamMembersAuditSnapshot(List.of(
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210002", false, "디자인"),
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210001", false, "개발")
        ));
        TeamMembersAuditSnapshot snapshot2 = new TeamMembersAuditSnapshot(List.of(
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210001", false, "개발"),
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210002", false, "디자인")
        ));

        // when
        boolean result = snapshot1.projectRolesEqual(snapshot2);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("팀장 여부는 비교하지 않는다")
    void ignoresLeaderFlag() {
        // given
        TeamMembersAuditSnapshot snapshot1 = new TeamMembersAuditSnapshot(List.of(
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210001", true, "개발"),
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210002", false, "디자인")
        ));
        TeamMembersAuditSnapshot snapshot2 = new TeamMembersAuditSnapshot(List.of(
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210001", false, "개발"),
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210002", true, "디자인")
        ));

        // when
        boolean result = snapshot1.projectRolesEqual(snapshot2);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("학번이 다르면 false를 반환한다")
    void returnsFalseWhenStudentNumbersAreDifferent() {
        // given
        TeamMembersAuditSnapshot snapshot1 = new TeamMembersAuditSnapshot(List.of(
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210001", false, "개발"),
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210002", false, "디자인")
        ));
        TeamMembersAuditSnapshot snapshot2 = new TeamMembersAuditSnapshot(List.of(
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210001", false, "개발"),
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210003", false, "디자인")
        ));

        // when
        boolean result = snapshot1.projectRolesEqual(snapshot2);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("멤버 수가 다르면 false를 반환한다")
    void returnsFalseWhenMemberCountIsDifferent() {
        // given
        TeamMembersAuditSnapshot snapshot1 = new TeamMembersAuditSnapshot(List.of(
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210001", false, "개발"),
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210002", false, "디자인")
        ));
        TeamMembersAuditSnapshot snapshot2 = new TeamMembersAuditSnapshot(List.of(
            new TeamMembersAuditSnapshot.TeamMemberAuditSnapshot("20210001", false, "개발")
        ));

        // when
        boolean result = snapshot1.projectRolesEqual(snapshot2);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 리스트끼리 비교하면 true를 반환한다")
    void returnsTrueWhenBothListsAreEmpty() {
        // given
        TeamMembersAuditSnapshot snapshot1 = new TeamMembersAuditSnapshot(List.of());
        TeamMembersAuditSnapshot snapshot2 = new TeamMembersAuditSnapshot(List.of());

        // when
        boolean result = snapshot1.projectRolesEqual(snapshot2);

        // then
        assertThat(result).isTrue();
    }
}
