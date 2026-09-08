package kgu.developers.domain.project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

/**
 * 제안서 섹션별 담당·작성 완료 상태. 팀원 동의(ProjectApproval)와는 다른 개념이다.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ProposalSection {
    private Long id;

    private Long projectId;
    private ProposalSectionType type;

    private String assigneeUserId;  // 담당 팀원 학번 (미지정이면 null)
    private LocalDateTime completedAt;  // 작성 완료 시각 (미완료면 null)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProposalSection create(Long projectId, ProposalSectionType type) {
        return ProposalSection.builder()
                .projectId(requireNonNull(projectId, "projectId"))
                .type(requireNonNull(type, "type"))
                .build();
    }

    public void assign(String assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public void updateCompleted(boolean completed) {
        if (completed == isCompleted()) {
            return;
        }
        this.completedAt = completed ? LocalDateTime.now() : null;
    }

    public void forceIncomplete() {
        this.completedAt = null;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }
}
