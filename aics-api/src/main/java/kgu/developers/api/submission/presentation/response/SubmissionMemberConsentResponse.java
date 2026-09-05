package kgu.developers.api.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record SubmissionMemberConsentResponse(

        @Schema(description = "지금 버전을 확인한 활성 학생 팀원 수", example = "3", requiredMode = REQUIRED)
        int confirmedCount,

        @Schema(description = "확인 대상 활성 학생 팀원 총원", example = "5", requiredMode = REQUIRED)
        int totalCount,

        @Schema(description = "요청한 본인이 확인했는지 여부", example = "true", requiredMode = REQUIRED)
        boolean isConfirmedByMe
) {
        public static SubmissionMemberConsentResponse of(int confirmedCount, int totalCount, boolean isConfirmedByMe) {
                return SubmissionMemberConsentResponse.builder()
                        .confirmedCount(confirmedCount)
                        .totalCount(totalCount)
                        .isConfirmedByMe(isConfirmedByMe)
                        .build();
        }
}
