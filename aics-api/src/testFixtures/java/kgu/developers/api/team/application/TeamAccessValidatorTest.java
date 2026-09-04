package kgu.developers.api.team.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.domain.TeamMember;
import mock.repository.FakeSectionRepository;
import mock.repository.FakeTeamMemberRepository;
import mock.repository.FakeTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

public class TeamAccessValidatorTest {

    private static final String MEMBER_ID = "202412345";
    private static final String LEADER_ID = "202412346";
    private static final String OUTSIDER = "202400000";
    private static final String PROFESSOR_ID = "PROF123";
    private static final Long TEAM_ID = 1L;
    private static final Long SECTION_ID = 1L;

    private TeamAccessValidator teamAccessValidator;
    private FakeTeamMemberRepository fakeTeamMemberRepository;
    private FakeTeamRepository fakeTeamRepository;
    private FakeSectionRepository fakeSectionRepository;

    @BeforeEach
    void init() {
        fakeTeamMemberRepository = new FakeTeamMemberRepository();
        fakeTeamRepository = new FakeTeamRepository();
        fakeSectionRepository = new FakeSectionRepository();

        fakeTeamMemberRepository.save(TeamMember.create(TEAM_ID, MEMBER_ID, false, "개발자"));
        fakeTeamMemberRepository.save(TeamMember.create(TEAM_ID, LEADER_ID, true, "팀장"));

        fakeTeamRepository.save(Team.builder().id(TEAM_ID).sectionId(SECTION_ID).build());
        fakeSectionRepository.save(Section.builder().id(SECTION_ID).professorId(PROFESSOR_ID).build());

        teamAccessValidator = new TeamAccessValidator(
            fakeTeamRepository,
            fakeTeamMemberRepository,
            fakeSectionRepository
        );
    }

    @Test
    @DisplayName("validateMembership는 팀원이면 예외를 던지지 않는다")
    void validateMembership_teamMember_passes() {
        assertDoesNotThrow(() -> teamAccessValidator.validateMembership(TEAM_ID, MEMBER_ID));
    }

    @Test
    @DisplayName("validateMembership는 팀원이 아니면 AccessDeniedException을 던진다")
    void validateMembership_nonMember_throwsAccessDenied() {
        assertThatThrownBy(() -> teamAccessValidator.validateMembership(TEAM_ID, OUTSIDER))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("해당 팀에 소속된 사용자만 접근할 수 있습니다.");
    }

    @Test
    @DisplayName("validateTeamLeader는 팀장이면 예외를 던지지 않는다")
    void validateTeamLeader_teamLeader_passes() {
        assertDoesNotThrow(() -> teamAccessValidator.validateTeamLeader(TEAM_ID, LEADER_ID));
    }

    @Test
    @DisplayName("validateTeamLeader는 팀원이 아니면 AccessDeniedException을 던진다")
    void validateTeamLeader_nonMember_throwsAccessDenied() {
        assertThatThrownBy(() -> teamAccessValidator.validateTeamLeader(TEAM_ID, OUTSIDER))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("팀장만 이 작업을 수행할 수 있습니다.");
    }

    @Test
    @DisplayName("validateTeamLeader는 팀장이 아닌 팀원이면 AccessDeniedException을 던진다")
    void validateTeamLeader_nonLeaderMember_throwsAccessDenied() {
        assertThatThrownBy(() -> teamAccessValidator.validateTeamLeader(TEAM_ID, MEMBER_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("팀장만 이 작업을 수행할 수 있습니다.");
    }

    @Test
    @DisplayName("validateMembershipOrProfessor는 팀원이면 예외를 던지지 않는다")
    void validateMembershipOrProfessor_teamMember_passes() {
        assertDoesNotThrow(() -> teamAccessValidator.validateMembershipOrProfessor(TEAM_ID, MEMBER_ID));
    }

    @Test
    @DisplayName("validateMembershipOrProfessor는 담당 교수이면 예외를 던지지 않는다")
    void validateMembershipOrProfessor_professor_passes() {
        assertDoesNotThrow(() -> teamAccessValidator.validateMembershipOrProfessor(TEAM_ID, PROFESSOR_ID));
    }

    @Test
    @DisplayName("validateMembershipOrProfessor는 팀원도 아니고 담당 교수도 아니면 AccessDeniedException을 던진다")
    void validateMembershipOrProfessor_nonMemberNonProfessor_throwsAccessDenied() {
        assertThatThrownBy(() -> teamAccessValidator.validateMembershipOrProfessor(TEAM_ID, OUTSIDER))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("해당 팀에 소속된 사용자 또는 담당 교수만 접근할 수 있습니다.");
    }
}