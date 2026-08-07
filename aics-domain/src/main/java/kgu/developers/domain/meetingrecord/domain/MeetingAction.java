package kgu.developers.domain.meetingrecord.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MeetingAction {

    private Long id;
    private Long meetingRecordId;
    private String assigneeId;
    private String content;
    private MeetingActionStatus status;
    private LocalDateTime dueAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 담당자 지정을 강제하지 않는다는 설계 원칙에 따라 assigneeId는 nullable이다.
    public static MeetingAction create(
        Long meetingRecordId,
        String assigneeId,
        String content,
        MeetingActionStatus status,
        LocalDateTime dueAt
    ) {
        return MeetingAction.builder()
            .meetingRecordId(meetingRecordId)
            .assigneeId(assigneeId)
            .content(content)
            .status(status)
            .dueAt(dueAt)
            .build();
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateStatus(MeetingActionStatus status) {
        this.status = status;
    }

    public void updateDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public void updateAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }
}
