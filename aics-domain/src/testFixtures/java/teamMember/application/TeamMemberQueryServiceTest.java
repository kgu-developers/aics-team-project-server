package teamMember.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teamMember.exception.TeamMemberNotFoundException;

@ExtendWith(MockitoExtension.class)
class TeamMemberQueryServiceTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private TeamMemberQueryService teamMemberQueryService;

    private TeamMember member(String userId, boolean isLeader) {
        return TeamMember.builder().id(1L).teamId(1L).userId(userId).isLeader(isLeader).build();
    }

    @Test
    @DisplayName("팀 ID와 학번으로 팀원을 조회한다")
    void getTeamMember() {
        given(teamMemberRepository.findByTeamIdAndUserId(1L, "202699999"))
                .willReturn(Optional.of(member("202699999", true)));

        assertThat(teamMemberQueryService.getTeamMember(1L, "202699999").getUserId())
                .isEqualTo("202699999");
    }

    @Test
    @DisplayName("없는 팀원을 조회하면 예외를 던진다")
    void getMissingTeamMember() {
        given(teamMemberRepository.findByTeamIdAndUserId(1L, "202600000")).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamMemberQueryService.getTeamMember(1L, "202600000"))
                .isInstanceOf(TeamMemberNotFoundException.class);
    }

    @Test
    @DisplayName("팀원 목록을 팀장 우선, 학번 순으로 정렬한다")
    void getTeamMembersByTeamId() {
        given(teamMemberRepository.findAllByTeamId(1L)).willReturn(List.of(
                member("202622222", false),
                member("202611111", false),
                member("202633333", true)));

        assertThat(teamMemberQueryService.getTeamMembersByTeamId(1L))
                .extracting(TeamMember::getUserId)
                .containsExactly("202633333", "202611111", "202622222");
    }

    @Test
    @DisplayName("팀원이 없으면 빈 목록을 반환한다")
    void getEmptyTeamMembers() {
        given(teamMemberRepository.findAllByTeamId(2L)).willReturn(List.of());

        assertThat(teamMemberQueryService.getTeamMembersByTeamId(2L)).isEmpty();
    }
}
