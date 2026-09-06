package meetingrecord.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.meetingrecord.domain.MeetingParticipant;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeetingRecordTest {

    @Test
    @DisplayName("create는 참석자 학번 중복을 제거하여 회의록을 생성한다")
    void create_DedupesParticipants() {
        // given
        LocalDateTime meetingAt = LocalDateTime.of(2026, 8, 3, 14, 0);

        // when
        MeetingRecord meetingRecord = MeetingRecord.create(
            1L, "회의록 제목", MeetingPhase.PROPOSAL, "202412345", meetingAt, "온라인(Zoom)", "회의 내용",
            List.of("202412345", "202412345", "202412346")
        );

        // then
        assertThat(meetingRecord.getTeamId()).isEqualTo(1L);
        assertThat(meetingRecord.getPhase()).isEqualTo(MeetingPhase.PROPOSAL);
        assertThat(meetingRecord.getAuthorId()).isEqualTo("202412345");
        assertThat(meetingRecord.getParticipantCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("create는 참석자 목록이 null이면 빈 목록으로 생성한다")
    void create_NullParticipants_ResultsInEmptyList() {
        MeetingRecord meetingRecord = MeetingRecord.create(
            1L, "회의록 제목", MeetingPhase.PROPOSAL, "202412345", LocalDateTime.now(), "장소", "내용", null
        );

        assertThat(meetingRecord.getParticipantCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("updateMeetingAt/updateLocation/updatePhase/updateContent는 각 필드를 갱신한다")
    void updaters_UpdateFields() {
        // given
        MeetingRecord meetingRecord = MeetingRecord.create(
            1L, "회의록 제목", MeetingPhase.PROPOSAL, "202412345", LocalDateTime.now(), "장소", "내용", List.of("202412345")
        );
        LocalDateTime newMeetingAt = LocalDateTime.of(2026, 9, 1, 10, 0);

        // when
        meetingRecord.updateMeetingAt(newMeetingAt);
        meetingRecord.updateLocation("대면 회의실");
        meetingRecord.updatePhase(MeetingPhase.FINAL);
        meetingRecord.updateContent("수정된 내용");

        // then
        assertThat(meetingRecord.getMeetingAt()).isEqualTo(newMeetingAt);
        assertThat(meetingRecord.getLocation()).isEqualTo("대면 회의실");
        assertThat(meetingRecord.getPhase()).isEqualTo(MeetingPhase.FINAL);
        assertThat(meetingRecord.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("updateParticipants는 기존 참석자를 새 목록으로 완전히 교체한다")
    void updateParticipants_ReplacesExistingParticipants() {
        // given
        MeetingRecord meetingRecord = MeetingRecord.builder()
            .id(1L)
            .teamId(1L)
            .phase(MeetingPhase.PROPOSAL)
            .authorId("202412345")
            .participants(List.of(MeetingParticipant.create(1L, "202412345")))
            .build();

        // when
        meetingRecord.updateParticipants(List.of("202499999"));

        // then
        assertThat(meetingRecord.getParticipants()).hasSize(1);
        assertThat(meetingRecord.getParticipants().get(0).getUserId()).isEqualTo("202499999");
        assertThat(meetingRecord.getParticipants().get(0).getMeetingRecordId()).isEqualTo(1L);
    }
}
