package kgu.developers.domain.teamMember.application.query;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamMemberQueryService {

    private static final Comparator<TeamMember> MEMBER_ORDER =
            Comparator.comparing(TeamMember::isLeader).reversed().thenComparing(TeamMember::getUserId);

    private final TeamMemberRepository teamMemberRepository;

    public List<TeamMember> getTeamMembersByTeamId(Long teamId) {
        return teamMemberRepository.findAllByTeamId(teamId).stream()
                .sorted(MEMBER_ORDER)
                .toList();
    }
}
