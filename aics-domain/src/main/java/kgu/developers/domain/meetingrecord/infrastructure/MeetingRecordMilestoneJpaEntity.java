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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "\"meeting_record_milestone\"",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_meeting_record_milestone",
        columnNames = {"meeting_record_id", "milestone_id"}
    )
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class MeetingRecordMilestoneJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "meeting_record_id", nullable = false)
    private Long meetingRecordId;

    @Column(name = "milestone_id", nullable = false)
    private Long milestoneId;
}
