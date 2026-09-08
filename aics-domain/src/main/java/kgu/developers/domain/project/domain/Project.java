package kgu.developers.domain.project.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;
import java.util.Objects;

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
    private JsonNode dataConfiguration;  // 데이터 구성. [{name, description, expectedCount}, ...] 배열.
    private JsonNode screenConfiguration;   // 화면 구성. [{title, description, imageFileId}, ...] 배열이고 배열 순서가 곧 화면 순서다.
    private String repositoryUrl;  // 저장소 URL
    private JsonNode externalLinks;  // 외부 링크
    private ApprovalStatus approvalStatus;  // 승인 상태
    // 팀 운영방식(회의방식·팀규칙·역할분담)은 여기 두지 않는다. 킥오프(Team.kickoffRule·meetingSchedule,
    // team_member.project_role)가 단일 출처이고 제안서는 그걸 그대로 보여준다.
    private String projectSchedule;  // 팀 운영방식 - 진행 일정

    private LocalDateTime proposalCompletedAt;  // 제안 완료 시각
    private long proposalRevision;  // 동의 대상 제안서 리비전
    private Long version;  // 낙관적 잠금 버전
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static Project create(Long teamId, String title, String description, String goal, String repositoryUrl, JsonNode externalLinks, ApprovalStatus approvalStatus, Long topicCandidateId, JsonNode dataConfiguration, JsonNode screenConfiguration, String projectSchedule) {
        return Project.builder()
                .teamId(requireNonNull(teamId, "teamId"))
                .title(requireNonNull(title, "title"))
                .description(requireNonNull(description, "description"))
                .goal(requireNonNull(goal, "goal"))
                .dataConfiguration(requireNonNull(dataConfiguration, "dataConfiguration"))
                .screenConfiguration(requireNonNull(screenConfiguration, "screenConfiguration"))
                .repositoryUrl(repositoryUrl)
                .externalLinks(externalLinks)
                .approvalStatus(requireNonNull(approvalStatus, "approvalStatus"))
                .projectSchedule(projectSchedule)
                .topicCandidateId(topicCandidateId)
                .build();
    }

    public void updateTitle(String title) {
        this.title = requireNonNull(title, "title");
    }

    public void updateTopicCandidateId(Long topicCandidateId) {
        this.topicCandidateId = topicCandidateId;
    }

    public void updateDescription(String description) {
        this.description = requireNonNull(description, "description");
    }

    public void updateGoal(String goal) {
        this.goal = requireNonNull(goal, "goal");
    }

    public void updateDataConfiguration(JsonNode dataConfiguration) {
        this.dataConfiguration = requireNonNull(dataConfiguration, "dataConfiguration");
    }

    public void updateScreenConfiguration(JsonNode screenConfiguration) {
        this.screenConfiguration = requireNonNull(screenConfiguration, "screenConfiguration");
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

    public void updateProjectSchedule(String projectSchedule) {
        this.projectSchedule = projectSchedule;
    }

    public boolean isDataConfigurationChanged(JsonNode newDataConfiguration) {
        return !Objects.equals(this.dataConfiguration, newDataConfiguration);
    }

    public boolean isScreenConfigurationChanged(JsonNode newScreenConfiguration) {
        return !Objects.equals(this.screenConfiguration, newScreenConfiguration);
    }

    public boolean isProjectScheduleChanged(String newProjectSchedule) {
        return !Objects.equals(this.projectSchedule, newProjectSchedule);
    }

    public void completeProposal() {
        this.proposalCompletedAt = LocalDateTime.now();
        this.approvalStatus = ApprovalStatus.APPROVED;
    }

    /**
     * 제안서 내용이 바뀌면 리비전을 올려 이전 리비전의 동의를 무효화한다.
     */
    public void increaseProposalRevision() {
        this.proposalRevision++;
    }

    public boolean hasSameProposalContent(
        String title,
        String description,
        String goal,
        String repositoryUrl,
        JsonNode externalLinks,
        JsonNode dataConfiguration,
        JsonNode screenConfiguration,
        String projectSchedule
    ) {
        return Objects.equals(this.title, title)
            && Objects.equals(this.description, description)
            && Objects.equals(this.goal, goal)
            && Objects.equals(this.repositoryUrl, repositoryUrl)
            && Objects.equals(this.externalLinks, externalLinks)
            && Objects.equals(this.dataConfiguration, dataConfiguration)
            && Objects.equals(this.screenConfiguration, screenConfiguration)
            && Objects.equals(this.projectSchedule, projectSchedule);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 소프트 삭제된 프로젝트를 새 제안서로 되살린다.
     * 되살아난 제안서는 새 리비전이므로 이전 리비전의 동의는 모두 무효가 된다.
     */
    public void reactivate(String title, String description, String goal, String repositoryUrl, JsonNode externalLinks, ApprovalStatus approvalStatus, Long topicCandidateId, JsonNode dataConfiguration, JsonNode screenConfiguration, String projectSchedule) {
        if (this.deletedAt == null) {
            throw new IllegalStateException("삭제되지 않은 프로젝트는 복구할 수 없습니다.");
        }
        requireNonNull(title, "title");
        requireNonNull(description, "description");
        requireNonNull(goal, "goal");
        requireNonNull(approvalStatus, "approvalStatus");
        requireNonNull(dataConfiguration, "dataConfiguration");
        requireNonNull(screenConfiguration, "screenConfiguration");

        this.title = title;
        this.description = description;
        this.goal = goal;
        this.dataConfiguration = dataConfiguration;
        this.screenConfiguration = screenConfiguration;
        this.repositoryUrl = repositoryUrl;
        this.externalLinks = externalLinks;
        this.approvalStatus = approvalStatus;
        this.projectSchedule = projectSchedule;
        this.topicCandidateId = topicCandidateId;
        this.proposalCompletedAt = null;
        this.deletedAt = null;
        this.proposalRevision++;
    }
}
