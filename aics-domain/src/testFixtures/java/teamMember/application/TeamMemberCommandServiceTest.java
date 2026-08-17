package teamMember.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teamMember.exception.TeamMemberAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
class TeamMemberCommandServiceTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private TeamMemberCommandService teamMemberCommandService;

    private TeamMember teamMember() {
        return TeamMember.builder()
                .id(1L).teamId(1L).userId("202699999").isLeader(false).projectRole("백엔드")
                .build();
    }

    @Test
    @DisplayName("targetTeamId가 있으면 팀을 옮긴다")
    void moveToAnotherTeam() {
        TeamMember teamMember = teamMember();
        given(teamMemberRepository.findByTeamIdAndUserId(2L, "202699999")).willReturn(Optional.empty());
        given(teamMemberRepository.save(teamMember)).willReturn(teamMember);

        teamMemberCommandService.updateTeamMember(teamMember, 2L, null, null);

        assertThat(teamMember.getTeamId()).isEqualTo(2L);
        assertThat(teamMember.getProjectRole()).isEqualTo("백엔드");
    }

    @Test
    @DisplayName("옮길 팀에 이미 소속된 학생이면 예외를 던진다")
    void moveToTeamAlreadyJoined() {
        TeamMember teamMember = teamMember();
        given(teamMemberRepository.findByTeamIdAndUserId(2L, "202699999"))
                .willReturn(Optional.of(teamMember()));

        assertThatThrownBy(() -> teamMemberCommandService.updateTeamMember(teamMember, 2L, null, null))
                .isInstanceOf(TeamMemberAlreadyExistsException.class);

        assertThat(teamMember.getTeamId()).isEqualTo(1L);
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("null인 필드는 변경하지 않는다")
    void partialUpdateKeepsNullFields() {
        TeamMember teamMember = teamMember();
        given(teamMemberRepository.save(teamMember)).willReturn(teamMember);

        teamMemberCommandService.updateTeamMember(teamMember, null, null, true);

        assertThat(teamMember.getTeamId()).isEqualTo(1L);
        assertThat(teamMember.getProjectRole()).isEqualTo("백엔드");
        assertThat(teamMember.isLeader()).isTrue();
    }

    @Test
    @DisplayName("현재 팀과 같은 targetTeamId면 중복 조회 없이 역할만 바꾼다")
    void sameTeamIdSkipsDuplicateCheck() {
        TeamMember teamMember = teamMember();
        given(teamMemberRepository.save(teamMember)).willReturn(teamMember);

        teamMemberCommandService.updateTeamMember(teamMember, 1L, "프론트엔드", null);

        assertThat(teamMember.getProjectRole()).isEqualTo("프론트엔드");
        verify(teamMemberRepository, never()).findByTeamIdAndUserId(any(), any());
    }
}
