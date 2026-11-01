package kgu.developers.domain.teamMember.application.query;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Comparator;
import java.util.HashMap;
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

    /** 어드민 목록 화면에서 팀명 옆에 팀장 이름을 같이 보여주기 위한 일괄 조회. 팀장이 없는 팀은 결과 맵에서 빠진다. */
    public Map<Long, String> getLeaderNamesByTeamIds(List<Long> teamIds) {
        List<TeamMember> leaders = teamMemberRepository.findAllByTeamIdIn(teamIds).stream()
                .filter(TeamMember::isLeader)
                .toList();
        Map<String, String> namesByStudentNumber = userQueryService
                .getUsersByStudentNumbersIncludingDeleted(
                        leaders.stream().map(TeamMember::getUserId).distinct().toList())
                .stream()
                .collect(toMap(User::getStudentNumber, User::getName, (first, second) -> first));

        // namesByStudentNumber.get(...)이 null일 수 있어(해당 학번을 못 찾은 경우) Collectors.toMap은 못 쓴다
        // (toMap은 값이 null이면 NPE를 던진다).
        Map<Long, String> leaderNamesByTeamId = new HashMap<>();
        for (TeamMember leader : leaders) {
            leaderNamesByTeamId.putIfAbsent(leader.getTeamId(), namesByStudentNumber.get(leader.getUserId()));
        }
        return leaderNamesByTeamId;
    }
}
