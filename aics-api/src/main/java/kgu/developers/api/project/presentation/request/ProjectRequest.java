package kgu.developers.api.project.presentation.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record ProjectRequest(
    @Schema(description = "프로젝트 제목", example = "AI 기반 학습 도우미", requiredMode = REQUIRED)
    @NotBlank
    @Size(max = 200)
    String title,
    
    @Schema(description = "프로젝트 설명", example = "개인별 학습 기록을 분석하는 서비스", requiredMode = REQUIRED)
    @NotBlank 
    String description,
    
    @Schema(description = "프로젝트 목표", example = "학습 피드백 자동화", requiredMode = REQUIRED)
    @NotBlank 
    String goal,
    
    @Schema(description = "데이터 구성 (입력받을 데이터 종류 / 예상 데이터 개수 / 수집 방식). 미입력 상태는 빈 문자열로 보낸다.",
        example = "종류: 학습 로그, 예상 개수: 약 1만 건, 수집 방식: 자체 수집", requiredMode = REQUIRED)
    @NotNull
    String dataConfiguration,

    // DB가 NOT NULL이라 항상 보내야 한다. 등록할 화면이 없으면 빈 배열([])을 보낸다.
    @Schema(description = "화면 구성 (JSON 배열, 배열 순서가 화면 순서: [{title, description, imageFileId}]). "
        + "imageFileId는 우리 팀원이 업로드한 파일이어야 하며, 조회 응답에는 서버가 imageUrl(15분 만료 presigned URL)을 채워 내려준다. "
        + "요청에 imageUrl을 넣어도 저장되지 않는다.",
        example = "[{\"title\":\"홈\",\"description\":\"학습 현황 요약\",\"imageFileId\":1}]", requiredMode = REQUIRED)
    @NotNull
    JsonNode screenConfiguration,

    @Schema(description = "주요 기능 (JSON 배열: [{title, description}, ...])",
        example = "[{\"title\":\"학습 분석\",\"description\":\"AI가 학습 패턴을 분석합니다\"}]", requiredMode = REQUIRED)
    @NotNull
    JsonNode keyFeatures,

    @Schema(description = "시연 흐름 (JSON 배열: [{number, title}, ...]) - number로 정렬됨",
        example = "[{\"number\":1,\"title\":\"회원과 도서를 검색합니다.\"},{\"number\":2,\"title\":\"대여 후 반납 상태를 확인합니다.\"}]", requiredMode = REQUIRED)
    @NotNull
    JsonNode demoFlow,

    @Schema(description = "회의 방식", example = "매주 월요일 대면 회의")
    @Size(max = 200)
    String meetingStyle,

    @Schema(description = "저장소 URL", example = "https://github.com/kgu/project")
    @Size(max = 255)
    String repositoryUrl,
    
    @Schema(description = "외부 링크 목록(JSON)", example = "[{\"name\":\"Figma\",\"url\":\"https://figma.com/...\"}]")
    JsonNode externalLinks
) {
    // @NotNull은 JSON `null`을 못 막는다 — Jackson이 JsonNode 필드의 JSON null을 Java null이 아니라
    // NullNode로 역직렬화해서, 그대로 두면 jsonb에 `null` 리터럴이 저장된다(컬럼은 NOT NULL인데도).
    // 화면 구성은 순서 있는 목록이라 배열이어야 하고, 등록할 화면이 없으면 빈 배열을 보낸다.
    @JsonIgnore
    @AssertTrue(message = "화면 구성은 JSON 배열이어야 합니다.")
    public boolean isScreenConfigurationArray() {
        return screenConfiguration != null && screenConfiguration.isArray();
    }

    // imageFileId 소유권 검사(ProjectFacade)와 presigned URL 보강은 "각 원소가 객체이고 imageFileId가
    // 정수"라는 전제 위에서 돈다. 그 전제를 여기 입력 경계에서 400으로 걸러내야, 파사드가 이상한
    // 모양을 만났을 때 조용히 건너뛰고(=검사 없이 저장) 넘어가는 일이 안 생긴다.
    @JsonIgnore
    @AssertTrue(message = "화면 구성의 각 항목은 객체여야 하고 imageFileId는 정수여야 합니다.")
    public boolean isScreenConfigurationShapeValid() {
        if (screenConfiguration == null || !screenConfiguration.isArray()) {
            return true; // isScreenConfigurationArray가 이미 잡는다
        }
        for (JsonNode screen : screenConfiguration) {
            if (!screen.isObject()) {
                return false;
            }
            JsonNode imageFileId = screen.get("imageFileId");
            if (imageFileId != null && !imageFileId.isNull() && !imageFileId.isIntegralNumber()) {
                return false;
            }
        }
        return true;
    }
}
