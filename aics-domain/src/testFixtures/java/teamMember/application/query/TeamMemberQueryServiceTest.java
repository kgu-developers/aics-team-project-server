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

@ExtendWith(MockitoExtension.class)
class TeamMemberQueryServiceTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

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
}
