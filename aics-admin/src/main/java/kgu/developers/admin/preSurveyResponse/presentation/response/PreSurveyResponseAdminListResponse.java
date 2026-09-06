package kgu.developers.admin.preSurveyResponse.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.user.domain.User;

@Builder
public record PreSurveyResponseAdminListResponse(

        @Schema(description = "분반 학생 사전조사 응답 목록", requiredMode = REQUIRED)
        List<PreSurveyResponseAdminResponse> contents
) {

    // 응답을 남긴 뒤 탈퇴한 학생은 user 행이 사라져 이름을 찾을 수 없다. 응답 자체는 그대로 보여줘야 하므로 이름만 대체 문구로 채운다.
    // 엑셀 내보내기(PreSurveyResponseExcelWriter)도 같은 문구를 쓰도록 공개한다 — 두 화면의 표기가 갈리면 안 된다.
    public static final String WITHDRAWN_USER_NAME = "(탈퇴한 사용자)";

    public static PreSurveyResponseAdminListResponse from(List<PreSurveyResponse> responses, List<User> users) {
        Map<String, String> names = users.stream()
                .collect(toMap(User::getStudentNumber, User::getName));
        return PreSurveyResponseAdminListResponse.builder()
                .contents(responses.stream()
                        .map(response -> PreSurveyResponseAdminResponse.from(
                                response, names.getOrDefault(response.getUserId(), WITHDRAWN_USER_NAME)))
                        .toList())
                .build();
    }
}
