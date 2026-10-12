package kgu.developers.domain.team.application.command;

import static kgu.developers.domain.auditLog.domain.AuditLogEventType.TEAM_UPDATED;
import static kgu.developers.domain.team.domain.Status.CONFIRMED;
import static kgu.developers.domain.team.domain.Status.FORMING;
import static kgu.developers.domain.team.domain.Team.MIN_UNFINALIZE_REASON_LENGTH;
import static kgu.developers.domain.team.domain.Team.MAX_UNFINALIZE_REASON_LENGTH;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.auditLog.application.command.AuditLogCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.DuplicateTeamNameException;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.team.exception.TeamInvalidUnfinalizeReasonException;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.submission.domain.SubmissionRepository;
import kgu.developers.domain.team.exception.TeamUnfinalizeCompletedFinalReportException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamCommandService {
    private final TeamQueryService teamQueryService;
    private final TeamRepository teamRepository;
    private final TransactionTemplate transactionTemplate;
    private final AuditLogCommandService auditLogCommandService;
    private final SectionRepository sectionRepository;
    private final MilestoneRepository milestoneRepository;
    private final SubmissionRepository submissionRepository;

    public Team updateKickoff(Long teamId, String name, String kickoffRule, String meetingSchedule) {
        Team team = teamQueryService.getTeamById(teamId);
        team.validateNotConfirmed();
        validateNameNotTaken(team, name);

        team.updateName(name);
        team.updateKickoffRule(kickoffRule);
        team.updateMeetingSchedule(meetingSchedule);

        return teamRepository.save(team);
    }

    /** 제안서 팀 운영방식은 확정 후에도 보완할 수 있지만, 팀명은 바꾸지 않는다. */
    public Team updateProposalKickoff(Long teamId, String kickoffRule, String meetingSchedule) {
        Team team = teamQueryService.getTeamById(teamId);
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
    public List<Team> finalizeTeams(Long sectionId, String actorId) {
        return transactionTemplate.execute(status -> {
            sectionRepository.findActiveByIdForUpdate(sectionId).orElseThrow(SectionNotFoundException::new);
            return teamRepository.findAllIdsBySectionId(sectionId).stream()
                    .map(teamId -> changeTeamStatus(teamId, actorId, CONFIRMED, "TEAM_FINALIZED", null))
                    .toList();
        });
    }

    @Transactional(propagation = NOT_SUPPORTED)
    public List<Team> unfinalizeTeams(Long sectionId, String actorId, String reason) {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.length() < MIN_UNFINALIZE_REASON_LENGTH
                || normalizedReason.length() > MAX_UNFINALIZE_REASON_LENGTH) {
            throw new TeamInvalidUnfinalizeReasonException();
        }
        return transactionTemplate.execute(status -> {
            // 최종보고서 완료와 같은 분반 행을 잠가 검증부터 전체 취소까지 직렬화한다.
            sectionRepository.findActiveByIdForUpdate(sectionId).orElseThrow(SectionNotFoundException::new);
            // 상태 엔티티는 미리 적재하지 않고 각 팀 잠금을 얻은 뒤 최신 값으로 읽는다.
            List<Long> teamIds = teamRepository.findAllIdsBySectionId(sectionId);
            Set<Long> teamIdSet = Set.copyOf(teamIds);
            boolean hasCompletedFinalReport = milestoneRepository.findAllBySectionIdOrderByWeekNumber(sectionId).stream()
                    .filter(milestone -> milestone.getType() == MilestoneType.FINAL_REPORT)
                    .flatMap(milestone -> submissionRepository.findAllByMilestoneId(milestone.getId()).stream())
                    .anyMatch(submission -> teamIdSet.contains(submission.getTeamId()) && submission.isCompleted());
            if (hasCompletedFinalReport) {
                throw new TeamUnfinalizeCompletedFinalReportException();
            }
            return teamIds.stream()
                    .map(teamId -> changeTeamStatus(teamId, actorId, FORMING, "TEAM_UNFINALIZED", normalizedReason))
                    .toList();
        });
    }

    private Team changeTeamStatus(Long teamId, String actorId, Status targetStatus, String changeType, String reason) {
        // 팀원 변경과 같은 행을 잠가 상태 변경과 수정 허용 여부가 일치하도록 한다.
        Team team = teamRepository.findByIdForUpdate(teamId)
                .orElseThrow(() -> new TeamNotFoundException());
        if (team.getStatus() == targetStatus) {
            return team;
        }
        Status before = team.getStatus();
        team.updateStatus(targetStatus);
        Team saved = teamRepository.save(team);
        Map<String, Object> metadata = new HashMap<>(Map.of(
                "changeType", changeType,
                "before", Map.of("status", before),
                "after", Map.of("status", targetStatus)));
        if (reason != null) {
            metadata.put("reason", reason);
        }
        auditLogCommandService.recordTeamChange(
                actorId, team.getSectionId(), teamId, TEAM_UPDATED, JsonConverter.toTree(metadata));
        return saved;
    }
}
