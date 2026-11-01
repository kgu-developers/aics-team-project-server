package kgu.developers.api.project.presentation.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest.MemberRole;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import kgu.developers.domain.project.domain.ProposalSectionType;
import lombok.Builder;
import java.util.List;

/** sectionKey로 지정한 섹션만 검증하고 저장한다. */
@Builder
@Schema(description = "sectionKey로 지정한 섹션만 저장한다. sectionKey·sectionVersion은 항상 필수이며, 본문 필드의 필수 여부는 섹션에 따라 다르다.")
public record ProjectRequest(
    @Schema(description = "저장할 섹션. 해당 섹션의 필드만 반영한다.", example = "DATA", requiredMode = REQUIRED)
    @NotNull
    ProposalSectionType sectionKey,

    @Schema(description = "편집 시작 시 GET 응답의 sectionVersions[sectionKey] 값. 저장 성공 후 응답의 새 버전으로 갱신한다.",
        example = "0", requiredMode = REQUIRED)
    @NotNull
    @PositiveOrZero
    Long sectionVersion,

    @Schema(description = "프로젝트 제목. TOPIC 저장 시 필수.", example = "AI 기반 학습 도우미", requiredMode = NOT_REQUIRED)
    String title,

    @Schema(description = "프로젝트 설명. TOPIC 저장 시 필수.", example = "개인별 학습 기록을 분석하는 서비스", requiredMode = NOT_REQUIRED)
    String description,

    @Schema(description = "프로젝트 목표. TOPIC 저장 시 필수.", example = "학습 피드백 자동화", requiredMode = NOT_REQUIRED)
    String goal,

    @Schema(description = "데이터 구성 (JSON 배열: [{name, description, expectedCount}]). DATA 저장 시 필수이며, 등록할 데이터가 없으면 빈 배열([])을 보낸다.",
        example = "[{\"name\":\"학습 로그\",\"description\":\"학생별 문제 풀이 기록\",\"expectedCount\":\"약 1만 건\"}]",
        requiredMode = NOT_REQUIRED)
    JsonNode dataConfiguration,

    @Schema(description = "화면 구성(JSON 배열, 배열 순서가 화면 순서: [{title, description, imageFileId}]). SCREEN 저장 시 필수이며, 등록할 화면이 없으면 빈 배열([])을 보낸다. "
        + "imageFileId는 우리 팀원이 업로드한 이미지 파일이어야 한다. 조회 응답에는 15분 만료 presigned imageUrl을 채워 내려주며, 요청에 imageUrl을 넣어도 저장하지 않는다.",
        example = "[{\"title\":\"홈\",\"description\":\"학습 현황 요약\",\"imageFileId\":1}]",
        requiredMode = NOT_REQUIRED)
    JsonNode screenConfiguration,

    @Schema(description = "팀 운영방식 - 팀규칙. TEAM_OPERATION 저장 시 사용하며, 생략/null이면 기존 값을 유지한다. 킥오프와 저장소를 공유한다.",
        example = "매주 화요일 회고", requiredMode = NOT_REQUIRED)
    String kickoffRule,

    @Schema(description = "팀 운영방식 - 회의시간·빈도·방식. TEAM_OPERATION 저장 시 사용하며, 생략/null이면 기존 값을 유지한다. 킥오프와 저장소를 공유한다.",
        example = "매주 목 19:00 온라인", requiredMode = NOT_REQUIRED)
    String meetingSchedule,

    @Schema(description = "팀 운영방식 - 역할분담. TEAM_OPERATION 저장 시 사용하며, 생략한 팀원의 역할은 유지된다. 킥오프 조회에도 반영된다.",
        example = "[{\"studentNumber\":\"202699999\",\"projectRole\":\"백엔드\"}]", requiredMode = NOT_REQUIRED)
    @Valid
    List<MemberRole> memberRoles,

    @Schema(description = "팀 운영방식 - 진행 일정. TEAM_OPERATION 저장 시 사용하며, 생략/null이면 유지하고 빈 문자열이면 비운다.",
        example = "4월: 요구사항 정리, 5월: 개발, 6월: 통합 테스트", requiredMode = NOT_REQUIRED)
    String projectSchedule,

    @Schema(description = "저장소 URL. TOPIC 저장 시 사용하며, 생략/null이면 비운다.", example = "https://github.com/kgu/project", requiredMode = NOT_REQUIRED)
    String repositoryUrl,

    @Schema(description = "외부 링크 목록(JSON). TOPIC 저장 시 사용하며, 생략/null이면 비운다.",
        example = "[{\"name\":\"Figma\",\"url\":\"https://figma.com/...\"}]", requiredMode = NOT_REQUIRED)
    JsonNode externalLinks
) {
    @JsonIgnore
    @AssertTrue(message = "TOPIC 섹션의 제목·설명·목표는 필수입니다.")
    public boolean isTopicContentValid() {
        return sectionKey != ProposalSectionType.TOPIC
            || (title != null && !title.isBlank() && description != null && !description.isBlank()
                && goal != null && !goal.isBlank());
    }

    @JsonIgnore
    @AssertTrue(message = "TOPIC 섹션의 제목은 200자 이하여야 합니다.")
    public boolean isTitleLengthValid() {
        return sectionKey != ProposalSectionType.TOPIC || title == null || title.length() <= 200;
    }

    @JsonIgnore
    @AssertTrue(message = "TOPIC 섹션의 저장소 URL은 255자 이하여야 합니다.")
    public boolean isRepositoryUrlLengthValid() {
        return sectionKey != ProposalSectionType.TOPIC || repositoryUrl == null || repositoryUrl.length() <= 255;
    }

    @JsonIgnore
    @AssertTrue(message = "데이터 구성은 JSON 배열이어야 합니다.")
    public boolean isDataConfigurationArray() {
        return sectionKey != ProposalSectionType.DATA
            || (dataConfiguration != null && dataConfiguration.isArray());
    }

    @JsonIgnore
    @AssertTrue(message = "화면 구성은 JSON 배열이어야 합니다.")
    public boolean isScreenConfigurationArray() {
        return sectionKey != ProposalSectionType.SCREEN
            || (screenConfiguration != null && screenConfiguration.isArray());
    }

    @JsonIgnore
    @AssertTrue(message = "화면 구성의 각 항목은 객체여야 하고 imageFileId는 정수여야 합니다.")
    public boolean isScreenConfigurationShapeValid() {
        if (sectionKey != ProposalSectionType.SCREEN || screenConfiguration == null || !screenConfiguration.isArray()) {
            return true;
        }
        for (JsonNode screen : screenConfiguration) {
            if (!screen.isObject()) return false;
            JsonNode imageFileId = screen.get("imageFileId");
            if (imageFileId != null && !imageFileId.isNull() && !imageFileId.isIntegralNumber()) return false;
        }
        return true;
    }
}
