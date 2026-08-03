package kgu.developers.api.teammessage.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import lombok.Builder;

@Builder
public record TeamMessageCreateRequest(

    // 인증 인프라(aics-auth)가 구현되기 전까지 발신자를 요청 바디로 직접 받는다.
    // 추후 SecurityContext의 인증 principal(학번)로 대체되어야 한다.
    @Schema(description = "발신자 학번", example = "202412345", requiredMode = REQUIRED)
    @NotBlank
    String senderId,

    @Schema(description = "메시지 관련 유형 (미지정 시 GENERAL)", example = "GENERAL", requiredMode = NOT_REQUIRED)
    TeamMessageRelatedType relatedType,

    @Schema(description = "관련 리소스 id (relatedType에 따라 의미가 달라지는 폴리모픽 참조, FK 아님)", example = "1", requiredMode = NOT_REQUIRED)
    Long relatedId,

    @Schema(description = "메시지 본문", example = "다음 회의 일정 문의드립니다.", requiredMode = REQUIRED)
    @NotBlank
    String message
) { }
