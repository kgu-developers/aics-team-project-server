package kgu.developers.domain.meetingrecord.infrastructure;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.meetingrecord.domain.MeetingParticipant;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"meeting_record\"")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class MeetingRecordJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teamId;

    @Column(nullable = false, length = 20)
    @Enumerated(STRING)
    private MeetingPhase phase;

    @Column(nullable = false, length = 20)
    private String authorId;

    @Column(nullable = false)
    private LocalDateTime meetingAt;

    @Column(length = 200)
    private String location;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    public MeetingRecord toDomain(List<MeetingParticipant> participants) {
        return MeetingRecord.builder()
            .id(this.id)
            .teamId(this.teamId)
            .phase(this.phase)
            .authorId(this.authorId)
            .meetingAt(this.meetingAt)
            .location(this.location)
            .content(this.content)
            .participants(participants)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .build();
    }

    public static MeetingRecordJpaEntity toEntity(MeetingRecord domain) {
        return MeetingRecordJpaEntity.builder()
            .id(domain.getId())
            .teamId(domain.getTeamId())
            .phase(domain.getPhase())
            .authorId(domain.getAuthorId())
            .meetingAt(domain.getMeetingAt())
            .location(domain.getLocation())
            .content(domain.getContent())
            .build();
    }
}
