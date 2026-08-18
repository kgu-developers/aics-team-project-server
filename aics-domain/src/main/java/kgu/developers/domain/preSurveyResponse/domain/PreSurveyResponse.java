package kgu.developers.domain.preSurveyResponse.domain;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.*;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

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

    private LocalDateTime submittedAt;  // 제출일
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static PreSurveyResponse create(String userId, Long sectionId, JsonNode preferredRoles,
                                           String topicOpinion, String etcOpinion) {
        return PreSurveyResponse.builder()
                .userId(requireNonNull(userId, "userId"))
                .sectionId(requireNonNull(sectionId, "sectionId"))
                .preferredRoles(requireNonNull(preferredRoles, "preferredRoles"))
                .topicOpinion(topicOpinion)
                .etcOpinion(etcOpinion)
                .submittedAt(LocalDateTime.now())
                .build();
    }

    public void update(JsonNode preferredRoles, String topicOpinion, String etcOpinion) {
        this.preferredRoles = requireNonNull(preferredRoles, "preferredRoles");
        this.topicOpinion = topicOpinion;
        this.etcOpinion = etcOpinion;
        this.submittedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
