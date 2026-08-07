package kgu.developers.domain.milestone.domain;

import lombok.Getter;

@Getter
public final class Milestone {
    private final Long id;
    private final Long sectionId;
    private String title;
    private String description;
    private int weekNumber;
    private MilestoneStatus status;
    private MilestoneSchedule schedule;

    private Milestone(
            Long id,
            Long sectionId,
            String title,
            String description,
            int weekNumber,
            MilestoneStatus status,
            MilestoneSchedule schedule
    ) {
        this.id = validateOptionalId(id);
        this.sectionId = validateSectionId(sectionId);
        this.title = validateTitle(title);
        this.description = normalizeDescription(description);
        this.weekNumber = validateWeekNumber(weekNumber);
        this.status = validateStatus(status);
        this.schedule = validateSchedule(schedule);
    }

    public static Milestone create(
            Long sectionId,
            String title,
            String description,
            int weekNumber,
            MilestoneSchedule schedule
    ) {
        return new Milestone(
                null,
                sectionId,
                title,
                description,
                weekNumber,
                MilestoneStatus.DRAFT,
                schedule
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
        if (id == null) {
            throw new IllegalArgumentException("마일스톤 식별자는 필수입니다.");
        }
        return new Milestone(id, sectionId, title, description, weekNumber, status, schedule);
    }

    public void updateDetails(String title, String description) {
        this.title = validateTitle(title);
        this.description = normalizeDescription(description);
    }

    public void updateSchedule(MilestoneSchedule schedule) {
        this.schedule = validateSchedule(schedule);
    }

    public void changeWeekNumber(int weekNumber) {
        this.weekNumber = validateWeekNumber(weekNumber);
    }

    public void changeStatus(MilestoneStatus status) {
        this.status = validateStatus(status);
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
        return title.trim();
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
}
