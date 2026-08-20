package kgu.developers.domain.teamMember.application.query;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teamMember.domain.TeamMemberWithUser;
import kgu.developers.domain.teamMember.exception.TeamMemberNotFoundException;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamMemberQueryService {
    private final TeamMemberRepository teamMemberRepository;
    private final UserQueryService userQueryService;

    public TeamMember getTeamMember(Long teamId, String studentNumber) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, studentNumber)
                .orElseThrow(TeamMemberNotFoundException::new);
    }

    public List<TeamMember> getTeamMembersByTeamId(Long teamId) {
        return teamMemberRepository.findAllByTeamId(teamId).stream()
                .sorted(Comparator.comparing(TeamMember::isLeader).reversed()
                        .thenComparing(TeamMember::getUserId))
                .toList();
    }

    public List<TeamMemberWithUser> getTeamMembersWithUsers(Long teamId) {
        List<TeamMember> members = getTeamMembersByTeamId(teamId);

        Map<String, User> users = userQueryService
                .getUsersByStudentNumbers(members.stream().map(TeamMember::getUserId).toList())
                .stream()
                .collect(toMap(User::getStudentNumber, identity()));

        return members.stream()
                .map(member -> new TeamMemberWithUser(member, users.get(member.getUserId())))
                .toList();
    }
}
