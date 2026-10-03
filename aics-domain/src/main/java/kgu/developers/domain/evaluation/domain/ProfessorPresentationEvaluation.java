package kgu.developers.domain.evaluation.domain;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ProfessorPresentationEvaluation {
    private final Long id;
    private final Long milestoneId;
    private final Long teamId;
    private String professorId;
    private String memo;
    private LocalDateTime submittedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime deletedAt;

    private ProfessorPresentationEvaluation(Long id, Long milestoneId, Long teamId,
            String professorId, String memo, LocalDateTime submittedAt,
            LocalDateTime createdAt, LocalDateTime deletedAt) {
        if (milestoneId == null || milestoneId <= 0 || teamId == null || teamId <= 0) {
            throw new IllegalArgumentException("발표 마일스톤과 팀 식별자는 필수입니다.");
        }
        this.id = id;
        this.milestoneId = milestoneId;
        this.teamId = teamId;
        this.professorId = professorId;
        this.memo = memo;
        this.submittedAt = submittedAt;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public static ProfessorPresentationEvaluation create(Long milestoneId, Long teamId) {
        return new ProfessorPresentationEvaluation(null, milestoneId, teamId, null, null, null, null, null);
    }

    public static ProfessorPresentationEvaluation restore(Long id, Long milestoneId, Long teamId,
            String professorId, String memo, LocalDateTime submittedAt,
            LocalDateTime createdAt, LocalDateTime deletedAt) {
        return new ProfessorPresentationEvaluation(id, milestoneId, teamId, professorId, memo,
                submittedAt, createdAt, deletedAt);
    }

    public void submit(String professorId, String memo, LocalDateTime submittedAt) {
        if (professorId == null || professorId.isBlank() || submittedAt == null) {
            throw new IllegalArgumentException("평가자와 제출 시각은 필수입니다.");
        }
        if (memo != null && memo.length() > 5000) {
            throw new IllegalArgumentException("교수자 메모는 5000자를 초과할 수 없습니다.");
        }
        this.professorId = professorId;
        this.memo = memo == null ? null : memo.trim();
        this.submittedAt = submittedAt;
    }
}
