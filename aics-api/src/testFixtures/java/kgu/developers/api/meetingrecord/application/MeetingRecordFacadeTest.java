package kgu.developers.api.meetingrecord.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordCreateRequest;
import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordUpdateRequest;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordDetailResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordListResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordPersistResponse;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.meetingrecord.application.command.MeetingRecordCommandService;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.FakeMeetingRecordRepository;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MeetingRecordFacadeTest {

    private MeetingRecordFacade meetingRecordFacade;

    @BeforeEach
    public void init() {
        FakeMeetingRecordRepository fakeMeetingRecordRepository = new FakeMeetingRecordRepository();
        meetingRecordFacade = new MeetingRecordFacade(
            new MeetingRecordCommandService(fakeMeetingRecordRepository),
            new MeetingRecordQueryService(fakeMeetingRecordRepository)
        );
    }

    @Test
    @DisplayName("createMeetingRecord는 참석자를 포함한 회의록을 생성한다")
    public void createMeetingRecord_Success() {
        // given
        MeetingRecordCreateRequest request = buildCreateRequest(MeetingPhase.MID_CHECK, List.of("202412345", "202412346"));

        // when
        MeetingRecordPersistResponse result = meetingRecordFacade.createMeetingRecord(1L, request);

        // then
        assertNotNull(result.id());
        assertEquals(MeetingPhase.MID_CHECK, result.phase());
        assertEquals("202412345", result.authorId());
    }

    @Test
    @DisplayName("createMeetingRecord는 중복된 참석자 학번을 제거하여 저장한다")
    public void createMeetingRecord_DedupesParticipants() {
        // given
        MeetingRecordCreateRequest request = buildCreateRequest(
            MeetingPhase.PROPOSAL,
            List.of("202412345", "202412345", "202412346")
        );

        // when
        MeetingRecordPersistResponse persisted = meetingRecordFacade.createMeetingRecord(1L, request);
        MeetingRecordDetailResponse detail = meetingRecordFacade.getMeetingRecord(persisted.id());

        // then
        assertEquals(2, detail.participantIds().size());
    }

    @Test
    @DisplayName("getMeetingRecords는 phase로 필터링한 목록을 반환한다")
    public void getMeetingRecords_FilterByPhase() {
        // given
        meetingRecordFacade.createMeetingRecord(1L, buildCreateRequest(MeetingPhase.PROPOSAL, List.of("202412345")));
        meetingRecordFacade.createMeetingRecord(1L, buildCreateRequest(MeetingPhase.FINAL, List.of("202412345")));

        // when
        MeetingRecordListResponse result = meetingRecordFacade.getMeetingRecords(1L, MeetingPhase.FINAL);

        // then
        assertEquals(1, result.contents().size());
        assertEquals(MeetingPhase.FINAL, result.contents().get(0).phase());
    }

    @Test
    @DisplayName("getMeetingRecord는 존재하지 않는 id면 예외를 던진다")
    public void getMeetingRecord_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> meetingRecordFacade.getMeetingRecord(999L))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("updateMeetingRecord는 participantIds가 주어지면 참석자 목록 전체를 치환한다")
    public void updateMeetingRecord_ReplacesParticipants() {
        // given
        MeetingRecordPersistResponse persisted = meetingRecordFacade.createMeetingRecord(
            1L, buildCreateRequest(MeetingPhase.PROPOSAL, List.of("202412345", "202412346"))
        );
        MeetingRecordUpdateRequest updateRequest = MeetingRecordUpdateRequest.builder()
            .participantIds(List.of("202499999"))
            .build();

        // when
        meetingRecordFacade.updateMeetingRecord(persisted.id(), updateRequest);
        MeetingRecordDetailResponse detail = meetingRecordFacade.getMeetingRecord(persisted.id());

        // then
        assertEquals(List.of("202499999"), detail.participantIds());
    }

    @Test
    @DisplayName("updateMeetingRecord는 participantIds가 주어지지 않으면 기존 참석자를 유지한다")
    public void updateMeetingRecord_KeepsParticipants_WhenNotProvided() {
        // given
        MeetingRecordPersistResponse persisted = meetingRecordFacade.createMeetingRecord(
            1L, buildCreateRequest(MeetingPhase.PROPOSAL, List.of("202412345", "202412346"))
        );
        MeetingRecordUpdateRequest updateRequest = MeetingRecordUpdateRequest.builder()
            .content("수정된 내용")
            .build();

        // when
        meetingRecordFacade.updateMeetingRecord(persisted.id(), updateRequest);
        MeetingRecordDetailResponse detail = meetingRecordFacade.getMeetingRecord(persisted.id());

        // then
        assertEquals(2, detail.participantIds().size());
        assertEquals("수정된 내용", detail.content());
    }

    @Test
    @DisplayName("deleteMeetingRecord는 회의록을 삭제한다")
    public void deleteMeetingRecord_Success() {
        // given
        MeetingRecordPersistResponse persisted = meetingRecordFacade.createMeetingRecord(
            1L, buildCreateRequest(MeetingPhase.PROPOSAL, List.of("202412345"))
        );

        // when
        meetingRecordFacade.deleteMeetingRecord(persisted.id());

        // then
        assertThatThrownBy(() -> meetingRecordFacade.getMeetingRecord(persisted.id()))
            .isInstanceOf(CustomException.class);
    }

    private MeetingRecordCreateRequest buildCreateRequest(MeetingPhase phase, List<String> participantIds) {
        return MeetingRecordCreateRequest.builder()
            .authorId("202412345")
            .meetingAt(LocalDateTime.of(2026, 8, 3, 14, 0))
            .location("온라인(Zoom)")
            .phase(phase)
            .content("회의 내용")
            .participantIds(participantIds)
            .build();
    }
}
