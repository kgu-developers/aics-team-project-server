package kgu.developers.domain.project.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class Project {
    private Long id;

    private Long teamId;  // 팀 식별자

    private String title;  // 제목
    private String description;  // 설명
    private String goal;  // 목표
    private String repositoryUrl;  // 저장소 URL
    private JsonNode externalLinks;  // 외부 링크
    private ApprovalStatus approvalStatus;  // 승인 상태
    private String meetingStyle;  // 회의방식

    private LocalDateTime proposalCompletedAt;  // 제안 완료 시각
    private long proposalRevision;  // 동의 대상 제안서 리비전
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static Project create(Long teamId, String title, String description, String goal, String repositoryUrl, JsonNode externalLinks, ApprovalStatus approvalStatus, String meetingStyle) {
        return Project.builder()
                .teamId(requireNonNull(teamId, "teamId"))
                .title(requireNonNull(title, "title"))
                .description(requireNonNull(description, "description"))
                .goal(requireNonNull(goal, "goal"))
                .repositoryUrl(repositoryUrl)
                .externalLinks(externalLinks)
                .approvalStatus(requireNonNull(approvalStatus, "approvalStatus"))
                .meetingStyle(meetingStyle)
                .build();
    }

    public void updateTitle(String title) {
        this.title = requireNonNull(title, "title");
    }

    public void updateDescription(String description) {
        this.description = requireNonNull(description, "description");
    }

    public void updateGoal(String goal) {
        this.goal = requireNonNull(goal, "goal");
    }

    public void updateRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public void updateExternalLinks(JsonNode externalLinks) {
        this.externalLinks = externalLinks;
    }

    public void updateApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = requireNonNull(approvalStatus, "approvalStatus");
    }

    public void updateMeetingStyle(String meetingStyle) {
        this.meetingStyle = meetingStyle;
    }

    public void completeProposal() {
        this.proposalCompletedAt = LocalDateTime.now();
        this.approvalStatus = ApprovalStatus.APPROVED;
    }

    public void increaseProposalRevision() {
        this.proposalRevision++;
    }

    public void restore() {
        this.deletedAt = null;
        this.proposalCompletedAt = null;
        this.approvalStatus = ApprovalStatus.DRAFT;
    }

    public boolean hasSameProposalContent(
        String title,
        String description,
        String goal,
        String meetingStyle,
        String repositoryUrl,
        JsonNode externalLinks
    ) {
        return java.util.Objects.equals(this.title, title)
            && java.util.Objects.equals(this.description, description)
            && java.util.Objects.equals(this.goal, goal)
            && java.util.Objects.equals(this.meetingStyle, meetingStyle)
            && java.util.Objects.equals(this.repositoryUrl, repositoryUrl)
            && java.util.Objects.equals(this.externalLinks, externalLinks);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
