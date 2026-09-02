package kgu.developers.domain.milestone.infrastructure;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "milestone",
        indexes = {
                @Index(
                        name = "idx_milestone_section_status_week",
                        columnList = "section_id, status, week_number"
                )
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class MilestoneJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(
            name = "section_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_milestone_section")
    )
    private SectionJpaEntity section;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @Enumerated(STRING)
    @Column(nullable = false, length = 16)
    private MilestoneStatus status;

    @Column(name = "opens_at")
    private LocalDateTime opensAt;

    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;

    @Column(name = "late_submission_until")
    private LocalDateTime lateSubmissionUntil;

    @Column(name = "revision_until")
    private LocalDateTime revisionUntil;

    @Column(name = "evaluation_opens_at")
    private LocalDateTime evaluationOpensAt;

    @Column(name = "evaluation_closes_at")
    private LocalDateTime evaluationClosesAt;

    public static MilestoneJpaEntity fromDomain(Milestone milestone) {
        MilestoneSchedule schedule = milestone.getSchedule();
        return MilestoneJpaEntity.builder()
                .id(milestone.getId())
                .sectionId(milestone.getSectionId())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .weekNumber(milestone.getWeekNumber())
                .status(milestone.getStatus())
                .opensAt(schedule.opensAt())
                .dueAt(schedule.dueAt())
                .lateSubmissionUntil(schedule.lateSubmissionUntil())
                .revisionUntil(schedule.revisionUntil())
                .evaluationOpensAt(schedule.evaluationOpensAt())
                .evaluationClosesAt(schedule.evaluationClosesAt())
                .build();
    }

    public void updateFromDomain(Milestone milestone) {
        if (!id.equals(milestone.getId())) {
            throw new IllegalArgumentException("다른 마일스톤의 정보로 갱신할 수 없습니다.");
        }
        if (!sectionId.equals(milestone.getSectionId())) {
            throw new IllegalArgumentException("마일스톤의 분반은 변경할 수 없습니다.");
        }

        MilestoneSchedule schedule = milestone.getSchedule();
        title = milestone.getTitle();
        description = milestone.getDescription();
        weekNumber = milestone.getWeekNumber();
        status = milestone.getStatus();
        opensAt = schedule.opensAt();
        dueAt = schedule.dueAt();
        lateSubmissionUntil = schedule.lateSubmissionUntil();
        revisionUntil = schedule.revisionUntil();
        evaluationOpensAt = schedule.evaluationOpensAt();
        evaluationClosesAt = schedule.evaluationClosesAt();
    }

    public Milestone toDomain() {
        return Milestone.restore(
                id,
                sectionId,
                title,
                description,
                weekNumber,
                status,
                new MilestoneSchedule(
                        opensAt,
                        dueAt,
                        lateSubmissionUntil,
                        revisionUntil,
                        evaluationOpensAt,
                        evaluationClosesAt
                )
        );
    }
}
