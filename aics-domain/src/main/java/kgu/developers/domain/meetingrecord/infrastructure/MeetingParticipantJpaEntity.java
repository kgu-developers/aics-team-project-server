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

    // MeetingRecord와 같은 애그리거트 소속이지만, 별도 Repository 경유 없이 조회/삭제하기 위해
    // 연관관계 매핑 대신 순수 id 컬럼으로 관리한다.
    @Column(name = "meeting_record_id", nullable = false)
    private Long meetingRecordId;

    // User(학번) 엔티티는 아직 구현되지 않아 순수 학번 문자열로만 참조한다.
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
