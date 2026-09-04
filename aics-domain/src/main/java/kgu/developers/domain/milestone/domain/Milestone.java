package kgu.developers.domain.milestone.domain;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public final class Milestone {
    private static final int MAX_TITLE_LENGTH = 100;

    private final Long id;
    private final Long sectionId;
    private String title;
    private String description;
    private int weekNumber;
    private MilestoneStatus status;
    private MilestoneSchedule schedule;
    private MilestoneType type;

    private Milestone(
            Long id,
            Long sectionId,
            String title,
            String description,
            int weekNumber,
            MilestoneStatus status,
            MilestoneSchedule schedule,
            MilestoneType type
    ) {
        this.id = validateOptionalId(id);
        this.sectionId = validateSectionId(sectionId);
        this.title = validateTitle(title);
        this.description = normalizeDescription(description);
        this.weekNumber = validateWeekNumber(weekNumber);
        this.status = validateStatus(status);
        this.schedule = validateSchedule(schedule);
        this.type = validateType(type);
    }

    // B3(제출·이력·발표)가 "이 마일스톤이 최종보고서/발표용인지" 구분하려고 추가한 필드.
    // 기존 호출부(B1)를 안 건드리려고 타입 없는 create/restore는 GENERAL로 기본값 처리한다.
    public static Milestone create(
            Long sectionId,
            String title,
            String description,
            int weekNumber,
            MilestoneSchedule schedule
    ) {
        return create(sectionId, title, description, weekNumber, schedule, MilestoneType.GENERAL);
    }

    public static Milestone create(
            Long sectionId,
            String title,
            String description,
            int weekNumber,
            MilestoneSchedule schedule,
            MilestoneType type
    ) {
        return new Milestone(
                null,
                sectionId,
                title,
                description,
                weekNumber,
                MilestoneStatus.DRAFT,
                schedule,
                type
        );
    }

    public static Milestone restore(
            Long id,
            Long sectionId,
            String title,
            String description,
            int weekNumber,
            MilestoneStatus status,
            MilestoneSchedule schedule
    ) {
        return restore(id, sectionId, title, description, weekNumber, status, schedule, MilestoneType.GENERAL);
    }

    public static Milestone restore(
            Long id,
            Long sectionId,
            String title,
            String description,
            int weekNumber,
            MilestoneStatus status,
            MilestoneSchedule schedule,
            MilestoneType type
    ) {
        if (id == null) {
            throw new IllegalArgumentException("마일스톤 식별자는 필수입니다.");
        }
        return new Milestone(id, sectionId, title, description, weekNumber, status, schedule, type);
    }

    public void updateDetails(String title, String description) {
        this.title = validateTitle(title);
        this.description = normalizeDescription(description);
    }

    public void updateSchedule(MilestoneSchedule schedule) {
        this.schedule = validateSchedule(schedule);
    }

    public void updateEvaluationWindow(
            LocalDateTime evaluationOpensAt,
            LocalDateTime evaluationClosesAt
    ) {
        this.schedule = schedule.withEvaluationWindow(evaluationOpensAt, evaluationClosesAt);
    }

    public void changeWeekNumber(int weekNumber) {
        this.weekNumber = validateWeekNumber(weekNumber);
    }

    public void changeStatus(MilestoneStatus status) {
        this.status = validateStatus(status);
    }

    public void changeType(MilestoneType type) {
        this.type = validateType(type);
    }

    public boolean belongsToSection(Long sectionId) {
        return this.sectionId.equals(sectionId);
    }

    private static Long validateOptionalId(Long id) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("마일스톤 식별자는 양수여야 합니다.");
        }
        return id;
    }

    private static Long validateSectionId(Long sectionId) {
        if (sectionId == null || sectionId <= 0) {
            throw new IllegalArgumentException("분반 식별자는 양수여야 합니다.");
        }
        return sectionId;
    }

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("마일스톤 제목은 필수입니다.");
        }

        String normalizedTitle = title.trim();
        if (normalizedTitle.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("마일스톤 제목은 100자를 초과할 수 없습니다.");
        }
        return normalizedTitle;
    }

    private static String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    private static int validateWeekNumber(int weekNumber) {
        if (weekNumber <= 0) {
            throw new IllegalArgumentException("주차는 양수여야 합니다.");
        }
        return weekNumber;
    }

    private static MilestoneStatus validateStatus(MilestoneStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("공개 상태는 필수입니다.");
        }
        return status;
    }

    private static MilestoneSchedule validateSchedule(MilestoneSchedule schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("마일스톤 일정은 필수입니다.");
        }
        return schedule;
    }

    private static MilestoneType validateType(MilestoneType type) {
        if (type == null) {
            throw new IllegalArgumentException("마일스톤 유형은 필수입니다.");
        }
        return type;
    }
}
