package kgu.developers.api.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;

@Builder
public record SubmissionSubmitterResponse(

        @Schema(description = "제출한 학번", example = "202412345", requiredMode = REQUIRED)
        String userId,

        @Schema(description = "제출한 사람 이름", example = "홍길동", requiredMode = REQUIRED)
        String name
) {
        public static SubmissionSubmitterResponse of(String userId, User user) {
                return SubmissionSubmitterResponse.builder()
                        .userId(userId)
                        .name(user == null ? null : user.getName())
                        .build();
        }
}
