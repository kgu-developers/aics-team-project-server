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
import jakarta.persistence.Version;
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

    @Version
    private long version;

    public MeetingAction toDomain() {
        return MeetingAction.builder()
            .id(this.id)
            .meetingRecordId(this.meetingRecordId)
            .assigneeId(this.assigneeId)
            .content(this.content)
            .status(this.status)
            .dueAt(this.dueAt)
            .version(this.version)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .build();
    }

    public static MeetingActionJpaEntity toEntity(MeetingAction domain) {
        MeetingActionJpaEntity entity = MeetingActionJpaEntity.builder()
            .id(domain.getId())
            .meetingRecordId(domain.getMeetingRecordId())
            .assigneeId(domain.getAssigneeId())
            .content(domain.getContent())
            .status(domain.getStatus())
            .dueAt(domain.getDueAt())
            .version(domain.getVersion())
            .build();
        // createdAt은 @Column(updatable = false)라 UPDATE SQL엔 안 들어가지만, 여기서 안 채워두면
        // merge() 직후 같은 트랜잭션 안에서 재조회한 도메인 객체의 createdAt이 null로 남는다.
        // MeetingActionFacade.updateMeetingAction()이 수정 직후 같은 트랜잭션에서 바로 재조회해
        // 응답을 만드는데, 그 응답(MeetingActionResponse.from())이 createdAt.format()을 null
        // 체크 없이 호출해서 NPE(500)로 이어졌다(KD3-233).
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }
}
