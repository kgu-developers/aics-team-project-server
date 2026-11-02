package teamMember.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;

@ExtendWith(MockitoExtension.class)
class TeamMemberQueryServiceTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private TeamMemberQueryService teamMemberQueryService;

    @Test
    @DisplayName("getTeamMembersByTeamId는 팀장을 먼저 두고 학번순으로 정렬한다")
    void getTeamMembersByTeamId() {
        TeamMember secondMember = member("202600002", false);
        TeamMember leader = member("202600003", true);
        TeamMember firstMember = member("202600001", false);
        given(teamMemberRepository.findAllByTeamId(1L))
                .willReturn(List.of(secondMember, leader, firstMember));

        List<TeamMember> result = teamMemberQueryService.getTeamMembersByTeamId(1L);

        assertThat(result)
                .extracting(TeamMember::getUserId)
                .containsExactly("202600003", "202600001", "202600002");
    }

    private TeamMember member(String userId, boolean leader) {
        return TeamMember.builder()
                .teamId(1L)
                .userId(userId)
                .isLeader(leader)
                .projectRole("개발")
                .build();
    }

    @Test
    @DisplayName("getLeaderNamesByTeamIds는 팀별 팀장 이름을 반환하고 팀장이 없는 팀은 결과에서 빠진다")
    void getLeaderNamesByTeamIds() {
        TeamMember leader1 = TeamMember.builder().teamId(10L).userId("202600001").isLeader(true).build();
        TeamMember member1 = TeamMember.builder().teamId(10L).userId("202600002").isLeader(false).build();
        TeamMember leader2 = TeamMember.builder().teamId(20L).userId("202600003").isLeader(true).build();
        TeamMember member2 = TeamMember.builder().teamId(30L).userId("202600004").isLeader(false).build();
        given(teamMemberRepository.findAllByTeamIdIn(List.of(10L, 20L, 30L)))
                .willReturn(List.of(leader1, member1, leader2, member2));
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(List.of("202600001", "202600003")))
                .willReturn(List.of(
                        User.builder().studentNumber("202600001").name("김철수").globalRole(UserGlobalRole.USER).build(),
                        User.builder().studentNumber("202600003").name("이영희").globalRole(UserGlobalRole.USER).build()));

        java.util.Map<Long, String> result = teamMemberQueryService.getLeaderNamesByTeamIds(List.of(10L, 20L, 30L));

        assertThat(result).containsEntry(10L, "김철수").containsEntry(20L, "이영희").doesNotContainKey(30L);
    }

    @Test
    @DisplayName("getLeaderNamesByTeamIds는 팀장의 이름을 못 찾아도 예외 없이 null 값으로 채운다")
    void getLeaderNamesByTeamIds_UserNotFound() {
        TeamMember leader = TeamMember.builder().teamId(10L).userId("202600001").isLeader(true).build();
        given(teamMemberRepository.findAllByTeamIdIn(List.of(10L))).willReturn(List.of(leader));
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(List.of("202600001")))
                .willReturn(List.of());

        java.util.Map<Long, String> result = teamMemberQueryService.getLeaderNamesByTeamIds(List.of(10L));

        assertThat(result).containsEntry(10L, null);
    }
}
