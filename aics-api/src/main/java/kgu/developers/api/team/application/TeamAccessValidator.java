package kgu.developers.api.team.application;

import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamAccessValidator {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SectionRepository sectionRepository;

    public void validateMembership(Long teamId, String userId) {
        if (teamMemberRepository.findByTeamIdAndUserId(teamId, userId).isEmpty()) {
            throw new AccessDeniedException("해당 팀에 소속된 사용자만 접근할 수 있습니다.");
        }
    }

    public Team validateMembershipOrProfessor(Long teamId, String userId) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new AccessDeniedException("해당 팀에 소속된 사용자 또는 담당 교수만 접근할 수 있습니다."));
        if (teamMemberRepository.findByTeamIdAndUserId(teamId, userId).isPresent()) {
            return team;
        }
        if (sectionRepository.existsActiveByIdAndProfessorId(team.getSectionId(), userId)) {
            return team;
        }
        throw new AccessDeniedException("해당 팀에 소속된 사용자 또는 담당 교수만 접근할 수 있습니다.");
    }

    public void validateTeamLeader(Long teamId, String userId) {
        boolean isLeader = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .map(member -> member.isLeader())
            .orElse(false);
        if (!isLeader) {
            throw new AccessDeniedException("팀장만 프로젝트 제안 단계를 완료할 수 있습니다.");
        }
    }
}
