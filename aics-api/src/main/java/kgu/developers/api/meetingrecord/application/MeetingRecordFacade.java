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
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
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

    public MeetingRecordListResponse getMeetingRecords(Long teamId, MeetingPhase phase, String userId) {
        validateTeamMembership(teamId, userId);
        return MeetingRecordListResponse.from(meetingRecordQueryService.getMeetingRecords(teamId, phase));
    }

    public MeetingRecordPersistResponse createMeetingRecord(Long teamId, String authorId, MeetingRecordCreateRequest request) {
        validateTeamMembership(teamId, authorId);
        Long id = meetingRecordCommandService.createMeetingRecord(
            teamId,
            request.phase(),
            authorId,
            request.meetingAt(),
            request.location(),
            request.content(),
            request.participantIds()
        );
        return MeetingRecordPersistResponse.of(meetingRecordQueryService.getMeetingRecord(id));
    }

    public MeetingRecordDetailResponse getMeetingRecord(Long id, String userId) {
        MeetingRecord meetingRecord = meetingRecordQueryService.getMeetingRecord(id);
        validateTeamMembership(meetingRecord.getTeamId(), userId);
        return MeetingRecordDetailResponse.from(meetingRecord);
    }

    public MeetingRecordPersistResponse updateMeetingRecord(Long id, MeetingRecordUpdateRequest request, String userId) {
        MeetingRecord meetingRecord = meetingRecordQueryService.getMeetingRecord(id);
        validateTeamMembership(meetingRecord.getTeamId(), userId);
        meetingRecordCommandService.updateMeetingRecord(
            id,
            request.meetingAt(),
            request.location(),
            request.phase(),
            request.content(),
            request.participantIds()
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
}
