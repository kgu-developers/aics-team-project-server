package kgu.developers.api.project.presentation.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest.MemberRole;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

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
    
    // DB가 NOT NULL이라 항상 보내야 한다. 등록할 데이터가 없으면 빈 배열([])을 보낸다.
    @Schema(description = "데이터 구성 (JSON 배열: [{name, description, expectedCount}])",
        example = "[{\"name\":\"학습 로그\",\"description\":\"학생별 문제 풀이 기록\",\"expectedCount\":\"약 1만 건\"}]",
        requiredMode = REQUIRED)
    @NotNull
    JsonNode dataConfiguration,

    // DB가 NOT NULL이라 항상 보내야 한다. 등록할 화면이 없으면 빈 배열([])을 보낸다.
    @Schema(description = "화면 구성 (JSON 배열, 배열 순서가 화면 순서: [{title, description, imageFileId}])"
        + "imageFileId는 우리 팀원이 업로드한 파일이어야 하며, 조회 응답에는 서버가 imageUrl(15분 만료 presigned URL)을 채워 내려준다. "
        + "요청에 imageUrl을 넣어도 저장되지 않는다.",
        example = "[{\"title\":\"홈\",\"description\":\"학습 현황 요약\",\"imageFileId\":1}]", requiredMode = REQUIRED)
    @NotNull
    JsonNode screenConfiguration,

    // 팀 운영방식의 팀규칙·회의방식·역할분담은 킥오프(Team, team_member)가 단일 출처다. 여기서 보낸 값은
    // 그 저장소에 그대로 쓰이므로 킥오프 조회에도 반영된다. 셋 다 선택값이고, 넘기지 않으면 지금 값을 유지한다.
    @Schema(description = "팀 운영방식 - 팀규칙. 넘기지 않으면 지금 값을 유지한다.", example = "매주 화요일 회고")
    String kickoffRule,

    @Schema(description = "팀 운영방식 - 회의시간·빈도·방식. 넘기지 않으면 지금 값을 유지한다.", example = "매주 목 19:00 온라인")
    String meetingSchedule,

    @Schema(description = "팀 운영방식 - 역할분담. 넘기지 않은 팀원의 역할은 유지된다.")
    @Valid
    List<MemberRole> memberRoles,

    @Schema(description = "팀 운영방식 - 진행 일정", example = "4월: 요구사항 정리, 5월: 개발, 6월: 통합 테스트")
    String projectSchedule,

    @Schema(description = "저장소 URL", example = "https://github.com/kgu/project")
    @Size(max = 255)
    String repositoryUrl,
    
    @Schema(description = "외부 링크 목록(JSON)", example = "[{\"name\":\"Figma\",\"url\":\"https://figma.com/...\"}]")
    JsonNode externalLinks
) {
    // @NotNull은 JSON `null`을 못 막는다 — Jackson이 JsonNode 필드의 JSON null을 Java null이 아니라
    // NullNode로 역직렬화해서, 그대로 두면 jsonb에 `null` 리터럴이 저장된다(컬럼은 NOT NULL인데도).
    // 두 구성 모두 순서 있는 목록이라 배열이어야 하고, 등록할 항목이 없으면 빈 배열을 보낸다.
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

    @JsonIgnore
    @AssertTrue(message = "데이터 구성은 JSON 배열이어야 합니다.")
    public boolean isDataConfigurationArray() {
        return dataConfiguration != null && dataConfiguration.isArray();
    }

}
