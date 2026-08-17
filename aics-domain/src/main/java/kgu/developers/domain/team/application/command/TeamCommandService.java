package kgu.developers.domain.team.application.command;

import static kgu.developers.domain.team.domain.Status.CONFIRMED;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
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

        team.updateName(name);
        team.updateTopic(topic);
        team.updateKickoffRule(kickoffRule);
        team.updateMeetingSchedule(meetingSchedule);

        return teamRepository.save(team);
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
