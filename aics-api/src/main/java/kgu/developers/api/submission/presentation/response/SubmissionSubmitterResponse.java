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
                // 제출 이력의 이름은 필수값이라 null을 내려보내면 안 된다. 소프트 삭제된 계정은
                // 호출부(SubmissionFacade.resolveSubmitters)가 삭제 포함 조회로 찾아오지만, 완전
                // 탈퇴(archiveAndHardDelete)로 계정 자체가 없어진 극단적인 경우에 대비한 안전망이다.
                return SubmissionSubmitterResponse.builder()
                        .userId(userId)
                        .name(user == null ? "(탈퇴한 사용자)" : user.getName())
                        .build();
        }
}
