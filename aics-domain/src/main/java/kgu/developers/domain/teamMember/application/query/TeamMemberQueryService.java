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
    private static final Comparator<TeamMember> MEMBER_ORDER =
            Comparator.comparing(TeamMember::isLeader).reversed().thenComparing(TeamMember::getUserId);

    private final TeamMemberRepository teamMemberRepository;
    private final UserQueryService userQueryService;

    public TeamMember getTeamMember(Long teamId, String studentNumber) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, studentNumber)
                .orElseThrow(TeamMemberNotFoundException::new);
    }

    public List<TeamMember> getTeamMembersByTeamId(Long teamId) {
        return teamMemberRepository.findAllByTeamId(teamId).stream()
                .sorted(MEMBER_ORDER)
                .toList();
    }

    public List<TeamMemberWithUser> getTeamMembersWithUsers(Long teamId) {
        return withUsers(getTeamMembersByTeamId(teamId));
    }

    /** 이미 팀원 목록을 들고 있는 호출자가 같은 조회를 반복하지 않도록 유저 결합만 따로 제공한다. */
    public List<TeamMemberWithUser> withUsers(List<TeamMember> teamMembers) {
        Map<String, User> users = userQueryService
                .getUsersByStudentNumbers(teamMembers.stream().map(TeamMember::getUserId).toList())
                .stream()
                .collect(toMap(User::getStudentNumber, identity()));

        return teamMembers.stream()
                .sorted(MEMBER_ORDER)
                .map(member -> new TeamMemberWithUser(member, users.get(member.getUserId())))
                .toList();
    }
}
