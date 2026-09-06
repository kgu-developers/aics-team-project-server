package team.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;

@ExtendWith(MockitoExtension.class)
class TeamAccessValidatorTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private TeamAccessValidator teamAccessValidator;

    @Test
    @DisplayName("활성 팀 소속이 없는 사용자의 팀 접근을 차단한다")
    void rejectsUserWithoutActiveMembership() {
        given(teamMemberRepository.findByTeamIdAndUserId(1L, "202699999"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> teamAccessValidator.validateMembership(1L, "202699999"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("활성 팀 소속이 있는 사용자의 팀 접근을 허용한다")
    void allowsUserWithActiveMembership() {
        TeamMember member = TeamMember.builder().id(1L).teamId(1L).userId("202699999").build();
        given(teamMemberRepository.findByTeamIdAndUserId(1L, "202699999"))
                .willReturn(Optional.of(member));

        assertThatCode(() -> teamAccessValidator.validateMembership(1L, "202699999"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("활성 팀 소속과 담당 교수 권한이 모두 없으면 교수 허용 API도 접근을 차단한다")
    void rejectsUserWithoutMembershipOrProfessorAccess() {
        Team team = Team.builder().id(1L).sectionId(10L).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeamIdAndUserId(1L, "202699999"))
                .willReturn(Optional.empty());
        given(sectionRepository.existsActiveByIdAndProfessorId(10L, "202699999"))
                .willReturn(false);

        assertThatThrownBy(() -> teamAccessValidator.validateMembershipOrProfessor(1L, "202699999"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
