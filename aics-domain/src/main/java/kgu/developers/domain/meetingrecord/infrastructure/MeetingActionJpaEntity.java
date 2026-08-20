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
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"meeting_action\"")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class MeetingActionJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "meeting_record_id", nullable = false)
    private Long meetingRecordId;

    @Column(name = "assignee_id", length = 20)
    private String assigneeId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, length = 20)
    @Enumerated(STRING)
    private MeetingActionStatus status;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    public MeetingAction toDomain() {
        return MeetingAction.builder()
            .id(this.id)
            .meetingRecordId(this.meetingRecordId)
            .assigneeId(this.assigneeId)
            .content(this.content)
            .status(this.status)
            .dueAt(this.dueAt)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .build();
    }

    public static MeetingActionJpaEntity toEntity(MeetingAction domain) {
        return MeetingActionJpaEntity.builder()
            .id(domain.getId())
            .meetingRecordId(domain.getMeetingRecordId())
            .assigneeId(domain.getAssigneeId())
            .content(domain.getContent())
            .status(domain.getStatus())
            .dueAt(domain.getDueAt())
            .build();
    }
}
