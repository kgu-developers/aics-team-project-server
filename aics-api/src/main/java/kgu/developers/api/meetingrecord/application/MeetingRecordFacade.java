package kgu.developers.api.meetingrecord.application;

import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordCreateRequest;
import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordUpdateRequest;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordDetailResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordListResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordPersistResponse;
import kgu.developers.domain.meetingrecord.application.command.MeetingRecordCommandService;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class MeetingRecordFacade {

    private final MeetingRecordCommandService meetingRecordCommandService;
    private final MeetingRecordQueryService meetingRecordQueryService;

    public MeetingRecordListResponse getMeetingRecords(Long teamId, MeetingPhase phase) {
        return MeetingRecordListResponse.from(meetingRecordQueryService.getMeetingRecords(teamId, phase));
    }

    public MeetingRecordPersistResponse createMeetingRecord(Long teamId, MeetingRecordCreateRequest request) {
        Long id = meetingRecordCommandService.createMeetingRecord(
            teamId,
            request.phase(),
            request.authorId(),
            request.meetingAt(),
            request.location(),
            request.content(),
            request.participantIds()
        );
        return MeetingRecordPersistResponse.of(meetingRecordQueryService.getMeetingRecord(id));
    }

    public MeetingRecordDetailResponse getMeetingRecord(Long id) {
        return MeetingRecordDetailResponse.from(meetingRecordQueryService.getMeetingRecord(id));
    }

    public MeetingRecordPersistResponse updateMeetingRecord(Long id, MeetingRecordUpdateRequest request) {
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

    public void deleteMeetingRecord(Long id) {
        meetingRecordCommandService.deleteMeetingRecord(id);
    }
}
