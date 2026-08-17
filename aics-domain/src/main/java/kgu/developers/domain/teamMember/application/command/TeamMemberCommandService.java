package kgu.developers.domain.teamMember.application.command;

import static kgu.developers.domain.team.domain.Status.CONFIRMED;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.exception.TeamAlreadyConfirmedException;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teamMember.exception.TeamMemberAlreadyExistsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamMemberCommandService {
    private final TeamMemberRepository teamMemberRepository;
    private final TeamQueryService teamQueryService;

    public TeamMember updateTeamMember(TeamMember teamMember, Long targetTeamId, String projectRole, Boolean isLeader) {
        requireNotConfirmed(teamQueryService.getTeamById(teamMember.getTeamId()));

        if (targetTeamId != null && !targetTeamId.equals(teamMember.getTeamId())) {
            requireNotConfirmed(teamQueryService.getTeamById(targetTeamId));
            teamMemberRepository.findByTeamIdAndUserId(targetTeamId, teamMember.getUserId())
                    .ifPresent(existing -> {
                        throw new TeamMemberAlreadyExistsException();
                    });
            teamMember.updateTeamId(targetTeamId);
        }
        if (projectRole != null) {
            teamMember.updateProjectRole(projectRole);
        }
        if (isLeader != null) {
            teamMember.updateIsLeader(isLeader);
        }
        return teamMemberRepository.save(teamMember);
    }

    private void requireNotConfirmed(Team team) {
        if (team.getStatus() == CONFIRMED) {
            throw new TeamAlreadyConfirmedException();
        }
    }
}
