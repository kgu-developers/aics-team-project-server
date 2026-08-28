package kgu.developers.domain.team.application.command;

import static kgu.developers.domain.team.domain.Status.CONFIRMED;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.DuplicateTeamNameException;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamCommandService {
    private final TeamQueryService teamQueryService;
    private final TeamRepository teamRepository;

    public Team updateKickoff(Long teamId, String name, String kickoffRule, String meetingSchedule) {
        Team team = teamQueryService.getTeamById(teamId);
        team.validateNotConfirmed();
        validateNameNotTaken(team, name);

        team.updateName(name);
        team.updateKickoffRule(kickoffRule);
        team.updateMeetingSchedule(meetingSchedule);

        return teamRepository.save(team);
    }

    private void validateNameNotTaken(Team team, String name) {
        boolean taken = teamRepository.findAllBySectionId(team.getSectionId()).stream()
                .anyMatch(other -> !other.getId().equals(team.getId()) && other.getName().equals(name));
        if (taken) {
            throw new DuplicateTeamNameException();
        }
    }

    public List<Team> finalizeTeams(Long sectionId) {
        List<Team> teams = new ArrayList<>(teamQueryService.getTeamsBySectionId(sectionId));
        
        teams.sort((t1, t2) -> Long.compare(t1.getId(), t2.getId()));
        
        List<Team> result = new ArrayList<>();
        for (Team team : teams) {
            if (team.getStatus() == CONFIRMED) {
                result.add(team);
                continue;
            }
            Team teamToFinalize = teamRepository.findById(team.getId())
                    .orElseThrow(() -> new TeamNotFoundException());
            teamToFinalize.validateNotConfirmed();
            teamToFinalize.updateStatus(CONFIRMED);
            result.add(teamRepository.save(teamToFinalize));
        }
        return result;
    }
}
