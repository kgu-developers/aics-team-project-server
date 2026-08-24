package mock.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import kgu.developers.domain.teamMember.domain.TeamMember;

class FakeTeamMemberRepositoryTest {

    @Test
    void findsOnlyActiveMemberInRequestedSection() {
        FakeTeamMemberRepository repository = new FakeTeamMemberRepository(Map.of(10L, 1L, 20L, 2L));
        TeamMember firstSectionMember = repository.save(TeamMember.create(10L, "202400001", false, ""));
        repository.save(TeamMember.create(20L, "202400001", false, ""));
        TeamMember deletedMember = repository.save(TeamMember.create(10L, "202400002", false, ""));
        repository.deleteById(deletedMember.getId());

        assertThat(repository.findActiveBySectionIdAndUserId(1L, "202400001"))
            .hasValueSatisfying(member -> assertThat(member.getId()).isEqualTo(firstSectionMember.getId()));
        assertThat(repository.findActiveBySectionIdAndUserId(1L, "202400002"))
            .isEmpty();
        assertThat(repository.findActiveBySectionIdAndUserId(3L, "202400001"))
            .isEmpty();
    }
}
