package kgu.developers.domain.team.application.command;

import static kgu.developers.domain.team.domain.Status.CONFIRMED;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

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
        if (teamRepository.existsBySectionIdAndNameAndIdNot(team.getSectionId(), name, team.getId())) {
            throw new DuplicateTeamNameException();
        }
    }

    @Transactional(propagation = NOT_SUPPORTED)
    public List<Team> finalizeTeams(Long sectionId) {
        teamQueryService.validateSectionExists(sectionId);

        return teamRepository.findAllBySectionId(sectionId).stream()
                .map(Team::getId)
                .sorted()
                .map(teamId -> transactionTemplate.execute(status -> finalizeTeam(teamId)))
                .toList();
    }

    private Team finalizeTeam(Long teamId) {
        // updateTeamMember 와 같은 Team 행을 잠가서, 확정 처리 중에 그 팀의
        // 팀원 정보가 바뀌는 경쟁 상태를 막는다(sunzx0428 리뷰 08-27 2번).
        Team team = teamRepository.findByIdForUpdate(teamId)
                .orElseThrow(() -> new TeamNotFoundException());
        if (team.getStatus() == CONFIRMED) {
            return team;
        }
        team.updateStatus(CONFIRMED);
        return teamRepository.save(team);
    }
}
