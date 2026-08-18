package kgu.developers.api.teammessage.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import lombok.Builder;

@Builder
public record TeamMessageCreateRequest(

    @Schema(description = "메시지 관련 유형 (미지정 시 GENERAL)", example = "GENERAL", requiredMode = NOT_REQUIRED)
    TeamMessageRelatedType relatedType,

    @Schema(description = "관련 리소스 id (relatedType에 따라 의미가 달라지는 폴리모픽 참조, FK 아님)", example = "1", requiredMode = NOT_REQUIRED)
    Long relatedId,

    @Schema(description = "메시지 본문", example = "다음 회의 일정 문의드립니다.", requiredMode = REQUIRED)
    @NotBlank
    String message
) { }
