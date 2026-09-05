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
    private Long topicCandidateId;  // 최종 확정 주제 후보 식별자

    private String title;  // 제목
    private String description;  // 설명
    private String goal;  // 목표
    private String repositoryUrl;  // 저장소 URL
    private JsonNode externalLinks;  // 외부 링크
    private ApprovalStatus approvalStatus;  // 승인 상태
    private String meetingStyle;  // 회의방식

    private LocalDateTime proposalCompletedAt;  // 제안 완료 시각
    private Long version;  // 낙관적 잠금 버전
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

    public void updateTopicCandidateId(Long topicCandidateId) {
        this.topicCandidateId = requireNonNull(topicCandidateId, "topicCandidateId");
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
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void reactivate(String title, String description, String goal, String repositoryUrl, JsonNode externalLinks, ApprovalStatus approvalStatus, String meetingStyle) {
        if (this.deletedAt == null) {
            throw new IllegalStateException("삭제되지 않은 프로젝트는 복구할 수 없습니다.");
        }
        requireNonNull(title, "title");
        requireNonNull(description, "description");
        requireNonNull(goal, "goal");
        requireNonNull(approvalStatus, "approvalStatus");

        this.title = title;
        this.description = description;
        this.goal = goal;
        this.repositoryUrl = repositoryUrl;
        this.externalLinks = externalLinks;
        this.approvalStatus = approvalStatus;
        this.meetingStyle = meetingStyle;
        this.topicCandidateId = null;
        this.proposalCompletedAt = null;
        this.deletedAt = null;
    }
}
