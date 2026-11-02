package kgu.developers.api.team.application;

import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
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

    // 아래 두 사전 검사는 팀 행을 잠그기 전에 팀 밖 사용자를 걸러낸다. 엔티티를 읽으면 잠금 후에도 오래된 값이
    // 영속성 컨텍스트에 남으므로 값만 조회한다. 잠근 뒤 validateMembership·validateMembershipOrProfessor로 다시 확인한다.
    public void validateMembershipBeforeLock(Long teamId, String userId) {
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new AccessDeniedException("해당 팀에 소속된 사용자만 접근할 수 있습니다.");
        }
    }

    public void validateMembershipOrProfessorBeforeLock(Long teamId, String userId) {
        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            return;
        }
        boolean professor = teamRepository.findSectionIdById(teamId)
            .filter(sectionId -> sectionRepository.existsActiveByIdAndProfessorId(sectionId, userId))
            .isPresent();
        if (!professor) {
            throw new AccessDeniedException("해당 팀에 소속된 사용자 또는 담당 교수만 접근할 수 있습니다.");
        }
    }

    public void validateLeader(Long teamId, String userId) {
        if (teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .filter(TeamMember::isLeader)
            .isEmpty()) {
            throw new AccessDeniedException("팀장만 이 작업을 수행할 수 있습니다.");
        }
    }

    public void validateLeaderWithTeamLock(Long teamId, String userId) {
        validateLeader(teamId, userId);
        teamRepository.findByIdForUpdate(teamId)
            .orElseThrow(TeamNotFoundException::new);
        validateLeader(teamId, userId);
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

}
