package kgu.developers.domain.team.application.command;

import static kgu.developers.domain.team.domain.Status.CONFIRMED;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.DuplicateTeamNameException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamCommandService {
    private final TeamQueryService teamQueryService;
    private final TeamRepository teamRepository;

    public Team updateKickoff(Long teamId, String name, String topic, String kickoffRule, String meetingSchedule) {
        Team team = teamQueryService.getTeamById(teamId);
        team.validateNotConfirmed();
        validateNameNotTaken(team, name);

        team.updateName(name);
        team.updateTopic(topic);
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
        return teamQueryService.getTeamsBySectionId(sectionId).stream()
                .map(team -> {
                    team.updateStatus(CONFIRMED);
                    return teamRepository.save(team);
                })
                .toList();
    }
}
