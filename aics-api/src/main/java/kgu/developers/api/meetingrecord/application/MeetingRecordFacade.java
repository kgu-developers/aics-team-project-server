package kgu.developers.api.meetingrecord.application;

import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordCreateRequest;
import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordUpdateRequest;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordDetailResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordListResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordPersistResponse;
import kgu.developers.domain.meetingrecord.application.command.MeetingRecordCommandService;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class MeetingRecordFacade {

    private final MeetingRecordCommandService meetingRecordCommandService;
    private final MeetingRecordQueryService meetingRecordQueryService;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final MilestoneRepository milestoneRepository;

    public MeetingRecordListResponse getMeetingRecords(Long teamId, MeetingPhase phase, String userId) {
        validateTeamMembership(teamId, userId);
        List<MeetingRecord> meetingRecords = meetingRecordQueryService.getMeetingRecords(teamId, phase);
        return MeetingRecordListResponse.from(meetingRecords, resolveMilestones(meetingRecords));
    }

    public MeetingRecordPersistResponse createMeetingRecord(Long teamId, String authorId, MeetingRecordCreateRequest request) {
        validateTeamMembership(teamId, authorId);
        List<Long> milestoneIds = validateMilestones(teamId, request.milestoneIds());
        Long id = meetingRecordCommandService.createMeetingRecord(
            teamId,
            request.title(),
            request.phase(),
            authorId,
            request.meetingAt(),
            request.location(),
            request.content(),
            request.participantIds(),
            milestoneIds
        );
        return MeetingRecordPersistResponse.of(meetingRecordQueryService.getMeetingRecord(id));
    }

    public MeetingRecordDetailResponse getMeetingRecord(Long id, String userId) {
        MeetingRecord meetingRecord = meetingRecordQueryService.getMeetingRecord(id);
        validateTeamMembership(meetingRecord.getTeamId(), userId);
        return MeetingRecordDetailResponse.from(meetingRecord, resolveMilestones(List.of(meetingRecord)));
    }

    public MeetingRecordPersistResponse updateMeetingRecord(Long id, MeetingRecordUpdateRequest request, String userId) {
        MeetingRecord meetingRecord = meetingRecordQueryService.getMeetingRecord(id);
        validateTeamMembership(meetingRecord.getTeamId(), userId);
        List<Long> milestoneIds = request.milestoneIds() == null
            ? null
            : validateMilestones(meetingRecord.getTeamId(), request.milestoneIds());
        meetingRecordCommandService.updateMeetingRecord(
            id,
            request.title(),
            request.meetingAt(),
            request.location(),
            request.phase(),
            request.content(),
            request.participantIds(),
            milestoneIds
        );
        return MeetingRecordPersistResponse.of(meetingRecordQueryService.getMeetingRecord(id));
    }

    public void deleteMeetingRecord(Long id, String userId) {
        MeetingRecord meetingRecord = meetingRecordQueryService.getMeetingRecord(id);
        validateTeamMembership(meetingRecord.getTeamId(), userId);
        meetingRecordCommandService.deleteMeetingRecord(id);
    }

    private void validateTeamMembership(Long teamId, String userId) {
        if (teamMemberRepository.findByTeamIdAndUserId(teamId, userId).isEmpty()) {
            throw new AccessDeniedException("해당 팀에 소속된 사용자만 회의록에 접근할 수 있습니다.");
        }
    }

    private List<Long> validateMilestones(Long teamId, List<Long> requestedMilestoneIds) {
        List<Long> milestoneIds = MeetingRecord.normalizeMilestoneIds(requestedMilestoneIds);
        if (milestoneIds.isEmpty()) {
            return milestoneIds;
        }
        Team team = teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);
        for (Long milestoneId : milestoneIds) {
            Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
            if (!milestone.belongsToSection(team.getSectionId())) {
                throw new AccessDeniedException("같은 분반의 마일스톤만 회의록에 연결할 수 있습니다.");
            }
        }
        return milestoneIds;
    }

    private Map<Long, Milestone> resolveMilestones(List<MeetingRecord> meetingRecords) {
        Map<Long, Milestone> milestonesById = new LinkedHashMap<>();
        meetingRecords.stream()
            .flatMap(meetingRecord -> meetingRecord.getMilestoneIds().stream())
            .distinct()
            .forEach(milestoneId -> milestoneRepository.findById(milestoneId)
                .ifPresent(milestone -> milestonesById.put(milestoneId, milestone)));
        return milestonesById;
    }
}
