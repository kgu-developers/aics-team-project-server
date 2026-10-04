package kgu.developers.domain.meetingrecord.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회의록 수정 사유는 덮어쓰지 않고 한 건씩 쌓는다(append-only). 수정·삭제 경로를 두지 않고,
// 회의록이 지워질 때만 MeetingRecordRepositoryImpl.deleteById()에서 함께 지운다.
@Entity
@Table(
    name = "\"meeting_record_edit_log\"",
    indexes = {
        @Index(name = "idx_meeting_record_edit_log_record", columnList = "meeting_record_id, created_at"),
        @Index(name = "idx_meeting_record_edit_log_team", columnList = "team_id, created_at")
    }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class MeetingRecordEditLogJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "meeting_record_id", nullable = false)
    private Long meetingRecordId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "editor_id", nullable = false, length = 20)
    private String editorId;

    @Column(nullable = false, length = 500)
    private String reason;

    public MeetingRecordEditLog toDomain() {
        return MeetingRecordEditLog.builder()
            .id(id)
            .meetingRecordId(meetingRecordId)
            .teamId(teamId)
            .editorId(editorId)
            .reason(reason)
            .createdAt(getCreatedAt())
            .build();
    }

    public static MeetingRecordEditLogJpaEntity toEntity(MeetingRecordEditLog meetingRecordEditLog) {
        return MeetingRecordEditLogJpaEntity.builder()
            .id(meetingRecordEditLog.getId())
            .meetingRecordId(meetingRecordEditLog.getMeetingRecordId())
            .teamId(meetingRecordEditLog.getTeamId())
            .editorId(meetingRecordEditLog.getEditorId())
            .reason(meetingRecordEditLog.getReason())
            .build();
    }
}
