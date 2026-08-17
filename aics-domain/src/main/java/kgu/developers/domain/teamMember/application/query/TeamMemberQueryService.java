package kgu.developers.domain.teamMember.application.query;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teamMember.exception.TeamMemberNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamMemberQueryService {
    private final TeamMemberRepository teamMemberRepository;

    public TeamMember getTeamMember(Long teamId, String studentNumber) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, studentNumber)
                .orElseThrow(TeamMemberNotFoundException::new);
    }

    /** 팀원 목록. 리포지토리가 정렬을 보장하지 않아 팀장 우선, 학번 순으로 맞춘다. */
    public List<TeamMember> getTeamMembersByTeamId(Long teamId) {
        return teamMemberRepository.findAllByTeamId(teamId).stream()
                .sorted(Comparator.comparing(TeamMember::isLeader).reversed()
                        .thenComparing(TeamMember::getUserId))
                .toList();
    }
}
