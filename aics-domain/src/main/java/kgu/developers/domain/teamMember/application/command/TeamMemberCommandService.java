package kgu.developers.domain.teamMember.application.command;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teamMember.exception.TeamMemberAlreadyExistsException;
import kgu.developers.domain.teamMember.exception.TeamMemberNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamMemberCommandService {
    private final TeamMemberRepository teamMemberRepository;
    private final TeamQueryService teamQueryService;

    public TeamMember updateTeamMember(TeamMember teamMember, Long targetTeamId, String projectRole, Boolean isLeader) {
        teamQueryService.getTeamById(teamMember.getTeamId()).validateNotConfirmed();

        if (targetTeamId != null && !targetTeamId.equals(teamMember.getTeamId())) {
            teamQueryService.getTeamById(targetTeamId).validateNotConfirmed();
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
            if (isLeader) {
                demoteCurrentLeader(teamMember);
            }
            teamMember.updateIsLeader(isLeader);
        }
        return teamMemberRepository.save(teamMember);
    }

    // 기존 팀장을 먼저 내려야 한 팀에 팀장이 둘이 되는 순간이 없다. 팀 이동 뒤에 호출되므로 옮겨간 팀이 기준이다.
    private void demoteCurrentLeader(TeamMember teamMember) {
        teamMemberRepository.findLeaderByTeamId(teamMember.getTeamId())
                .filter(leader -> !leader.getId().equals(teamMember.getId()))
                .ifPresent(leader -> {
                    leader.updateIsLeader(false);
                    teamMemberRepository.save(leader);
                });
    }

    public void updateKickoffRoles(Long teamId, String leaderStudentNumber, Map<String, String> projectRoles) {
        teamQueryService.getTeamById(teamId).validateNotConfirmed();

        Map<String, TeamMember> members = teamMemberRepository.findAllByTeamId(teamId).stream()
                .collect(toMap(TeamMember::getUserId, identity()));

        TeamMember leader = requireMember(members, leaderStudentNumber);
        projectRoles.keySet().forEach(studentNumber -> requireMember(members, studentNumber));

        // 기존 팀장을 먼저 내려야 한 팀에 팀장이 둘이 되는 순간이 없다
        members.values().stream()
                .filter(member -> member.isLeader() && !member.getUserId().equals(leaderStudentNumber))
                .forEach(member -> applyChanges(member, false, projectRoles));

        applyChanges(leader, true, projectRoles);

        members.values().stream()
                .filter(member -> !member.getUserId().equals(leaderStudentNumber))
                .forEach(member -> applyChanges(member, false, projectRoles));
    }

    private void applyChanges(TeamMember member, boolean isLeader, Map<String, String> projectRoles) {
        String projectRole = projectRoles.get(member.getUserId());
        boolean changed = member.isLeader() != isLeader
                || (projectRole != null && !projectRole.equals(member.getProjectRole()));
        if (!changed) {
            return;
        }

        member.updateIsLeader(isLeader);
        if (projectRole != null) {
            member.updateProjectRole(projectRole);
        }
        teamMemberRepository.save(member);
    }

    private TeamMember requireMember(Map<String, TeamMember> members, String studentNumber) {
        TeamMember member = members.get(studentNumber);
        if (member == null) {
            throw new TeamMemberNotFoundException();
        }
        return member;
    }
}
