package kgu.developers.domain.preSurveyResponse.domain;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.*;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

import java.util.Objects;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class PreSurveyResponse {
    private Long id;

    private String userId;  // 학번
    private Long sectionId;  // 분반 식별자

    private JsonNode preferredRoles;  // 희망 역할 (형식 제약 없음)
    private String topicOpinion;  // 주제 의견
    private String etcOpinion;  // 기타 의견

    private String preferredPeerUserId;  // 희망 조원 학번
    private PreferredPeerStatus preferredPeerStatus;  // 희망 조원 지목 상태

    private LocalDateTime submittedAt;  // 제출일
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static PreSurveyResponse create(String userId, Long sectionId, JsonNode preferredRoles,
                                           String topicOpinion, String etcOpinion, String preferredPeerUserId) {
        PreSurveyResponse response = PreSurveyResponse.builder()
                .userId(requireNonNull(userId, "userId"))
                .sectionId(requireNonNull(sectionId, "sectionId"))
                .preferredRoles(requireNonNull(preferredRoles, "preferredRoles"))
                .topicOpinion(topicOpinion)
                .etcOpinion(etcOpinion)
                .submittedAt(LocalDateTime.now())
                .build();
        response.updatePreferredPeer(preferredPeerUserId);
        return response;
    }

    public void update(JsonNode preferredRoles, String topicOpinion, String etcOpinion, String preferredPeerUserId) {
        this.preferredRoles = requireNonNull(preferredRoles, "preferredRoles");
        this.topicOpinion = topicOpinion;
        this.etcOpinion = etcOpinion;
        this.submittedAt = LocalDateTime.now();
        updatePreferredPeer(preferredPeerUserId);
    }

    /** 지목 대상이 그대로면 상대가 이미 내린 수락·거절을 재제출로 되돌리지 않는다. 대상이 바뀌면 다시 대기 상태로. */
    private void updatePreferredPeer(String preferredPeerUserId) {
        if (Objects.equals(this.preferredPeerUserId, preferredPeerUserId)) {
            return;
        }
        this.preferredPeerUserId = preferredPeerUserId;
        this.preferredPeerStatus = preferredPeerUserId == null ? null : PreferredPeerStatus.PENDING;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
