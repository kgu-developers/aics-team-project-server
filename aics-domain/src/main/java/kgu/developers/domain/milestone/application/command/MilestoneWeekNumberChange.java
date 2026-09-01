package kgu.developers.domain.milestone.application.command;

public record MilestoneWeekNumberChange(Long milestoneId, int weekNumber) {
    public MilestoneWeekNumberChange {
        if (milestoneId == null || milestoneId <= 0) {
            throw new IllegalArgumentException("마일스톤 식별자는 양수여야 합니다.");
        }
        if (weekNumber <= 0) {
            throw new IllegalArgumentException("주차는 양수여야 합니다.");
        }
    }
}
