package kgu.developers.domain.meetingrecord.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.meetingrecord.domain.MeetingParticipant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "\"meeting_participant\"",
    uniqueConstraints = @UniqueConstraint(name = "uq_meeting_participant", columnNames = {"meeting_record_id", "user_id"})
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class MeetingParticipantJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "meeting_record_id", nullable = false)
    private Long meetingRecordId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    public MeetingParticipant toDomain() {
        return MeetingParticipant.builder()
            .id(this.id)
            .meetingRecordId(this.meetingRecordId)
            .userId(this.userId)
            .build();
    }

    public static MeetingParticipantJpaEntity toEntity(MeetingParticipant domain) {
        return MeetingParticipantJpaEntity.builder()
            .id(domain.getId())
            .meetingRecordId(domain.getMeetingRecordId())
            .userId(domain.getUserId())
            .build();
    }
}
