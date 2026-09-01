package kgu.developers.domain.teamMember.application.command;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teamMember.exception.LeaderAlreadyExistsException;
import kgu.developers.domain.teamMember.exception.LeaderMoveRequiresExplicitRoleException;
import kgu.developers.domain.teamMember.exception.TeamMemberAlreadyExistsException;
import kgu.developers.domain.teamMember.exception.TeamMemberNotFoundException;
import kgu.developers.domain.teamMember.exception.TeamMemberSectionMismatchException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamMemberCommandService {
  private final TeamMemberRepository teamMemberRepository;
  private final TeamQueryService teamQueryService;
  private final TeamRepository teamRepository;

  public TeamMember updateTeamMember(TeamMember teamMember, Long targetTeamId, String projectRole, Boolean isLeader) {
    // Re-fetch current team to ensure version check
    Team currentTeam = teamRepository.findById(teamMember.getTeamId())
        .orElseThrow(() -> new TeamNotFoundException());
    validateUpdateAllowed(currentTeam, targetTeamId, projectRole, isLeader);

    boolean moved = targetTeamId != null && !targetTeamId.equals(teamMember.getTeamId());
    // 팀장을 옮기면 원래 팀이 팀장을 잃으므로 조용히 처리하지 않고 isLeader 를 명시하게 한다.
    if (moved && teamMember.isLeader() && isLeader == null) {
      throw new LeaderMoveRequiresExplicitRoleException();
    }
    // 팀장은 팀에 종속된 역할이라 이동만 요청하면 따라가지 않는다.
    boolean finalLeaderStatus = isLeader != null ? isLeader : (!moved && teamMember.isLeader());
    
    if (moved) {
      Team targetTeam = teamRepository.findById(targetTeamId)
          .orElseThrow(() -> new TeamNotFoundException());

      targetTeam.validateNotConfirmed();
      if (!currentTeam.getSectionId().equals(targetTeam.getSectionId())) {
        throw new TeamMemberSectionMismatchException();
      }
      teamMemberRepository.findByTeamIdAndUserId(targetTeamId, teamMember.getUserId())
          .ifPresent(existing -> {
            throw new TeamMemberAlreadyExistsException();
          });
      if (finalLeaderStatus) {
        validateNoLeaderInTeam(targetTeamId, teamMember.getId());
      }
      teamMember.updateTeamId(targetTeamId);
    }
    if (projectRole != null) {
      teamMember.updateProjectRole(projectRole);
    }
    if (finalLeaderStatus != teamMember.isLeader()) {
      if (finalLeaderStatus && !moved) {
        demoteCurrentLeader(teamMember);
      }
      teamMember.updateIsLeader(finalLeaderStatus);
    }
    return teamMemberRepository.save(teamMember);
  }

  public TeamMember claimLeader(Long teamId, String userId) {
    Team team = teamQueryService.getTeamById(teamId);
    team.validateNotConfirmed();
    TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
        .orElseThrow(TeamMemberNotFoundException::new);
    validateNoLeaderInTeam(teamId, member.getId());

    member.updateIsLeader(true);
    TeamMember claimed = teamMemberRepository.save(member);
    team.updateStatus(Status.CONFIRMED);
    teamRepository.save(team);
    return claimed;
  }

  private void validateUpdateAllowed(
      Team currentTeam, Long targetTeamId, String projectRole, Boolean isLeader) {
    if (currentTeam.getStatus() != Status.CONFIRMED) {
      return;
    }
    // 확정 이후에는 팀 이동·역할 변경만 막는다. 값이 없는 필드는 변경하지 않으므로
    // 빈 PATCH나 교수용 팀장 재배정(isLeader만 지정)은 통과시킨다.
    if (targetTeamId == null && projectRole == null) {
      return;
    }
    currentTeam.validateNotConfirmed();
  }

  private void validateNoLeaderInTeam(Long teamId, Long memberId) {
    teamMemberRepository.findLeaderByTeamId(teamId)
        .filter(leader -> !leader.getId().equals(memberId))
        .ifPresent(leader -> {
          throw new LeaderAlreadyExistsException();
        });
  }

  // 기존 팀장을 먼저 내려야 한 팀에 팀장이 둘이 되는 순간이 없다.
  private void demoteCurrentLeader(TeamMember teamMember) {
    teamMemberRepository.findLeaderByTeamId(teamMember.getTeamId())
        .filter(leader -> !leader.getId().equals(teamMember.getId()))
        .ifPresent(leader -> {
          leader.updateIsLeader(false);
          teamMemberRepository.save(leader);
        });
  }

  public List<TeamMember> updateKickoffRoles(Long teamId, String leaderStudentNumber, Map<String, String> projectRoles) {
    teamQueryService.getTeamById(teamId).validateNotConfirmed();

    Map<String, TeamMember> members = teamMemberRepository.findAllByTeamId(teamId).stream()
        .collect(toMap(TeamMember::getUserId, identity()));

    TeamMember leader = requireMember(members, leaderStudentNumber);
    projectRoles.keySet().forEach(studentNumber -> requireMember(members, studentNumber));

    // 기존 팀장을 먼저 내려야 한 팀에 팀장이 둘이 되는 순간이 없다.
    List<TeamMember> changedMembers = new ArrayList<>();
    Set<String> processedMembers = new HashSet<>();
    members.values().stream()
        .filter(member -> member.isLeader() && !member.getUserId().equals(leaderStudentNumber))
        .forEach(member -> {
          if (applyChanges(member, false, projectRoles)) {
            changedMembers.add(member);
          }
          processedMembers.add(member.getUserId());
        });

    if (applyChanges(leader, true, projectRoles)) {
      changedMembers.add(leader);
    }
    processedMembers.add(leaderStudentNumber);

    members.values().stream()
        .filter(member -> !processedMembers.contains(member.getUserId()))
        .forEach(member -> {
          if (applyChanges(member, false, projectRoles)) {
            changedMembers.add(member);
          }
        });

    if (!changedMembers.isEmpty()) {
      teamMemberRepository.saveAll(changedMembers)
          .forEach(saved -> members.put(saved.getUserId(), saved));
    }
    return List.copyOf(members.values());
  }

  private boolean applyChanges(TeamMember member, boolean isLeader, Map<String, String> projectRoles) {
    String projectRole = projectRoles.get(member.getUserId());
    boolean changed = member.isLeader() != isLeader
        || (projectRole != null && !projectRole.equals(member.getProjectRole()));
    if (!changed) {
      return false;
    }

    member.updateIsLeader(isLeader);
    if (projectRole != null) {
      member.updateProjectRole(projectRole);
    }
    return true;
  }

  private TeamMember requireMember(Map<String, TeamMember> members, String studentNumber) {
    TeamMember member = members.get(studentNumber);
    if (member == null) {
      throw new TeamMemberNotFoundException();
    }
    return member;
  }
}
