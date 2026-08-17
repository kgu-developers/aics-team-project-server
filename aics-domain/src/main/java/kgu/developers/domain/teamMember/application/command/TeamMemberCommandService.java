package kgu.developers.domain.teamMember.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teamMember.exception.TeamMemberAlreadyExistsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamMemberCommandService {
    private final TeamMemberRepository teamMemberRepository;

    /**
     * 팀원의 소속 팀과 역할을 부분 수정한다. null인 값은 변경하지 않는다.
     * 옮긴 팀에 이미 팀장이 있으면 save에서 LeaderAlreadyExistsException이 난다.
     */
    public TeamMember updateTeamMember(TeamMember teamMember, Long targetTeamId, String projectRole, Boolean isLeader) {
        if (targetTeamId != null && !targetTeamId.equals(teamMember.getTeamId())) {
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
            teamMember.updateIsLeader(isLeader);
        }
        return teamMemberRepository.save(teamMember);
    }
}
