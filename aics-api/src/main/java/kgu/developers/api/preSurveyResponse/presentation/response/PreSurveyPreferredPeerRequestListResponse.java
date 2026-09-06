package kgu.developers.api.preSurveyResponse.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreferredPeerStatus;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;

@Builder
public record PreSurveyPreferredPeerRequestListResponse(

    @Schema(description = "나를 조원으로 지목한 학생 목록", requiredMode = REQUIRED)
    List<ReceivedRequest> contents
) {

    // 지목한 뒤 탈퇴한 학생은 user 행이 사라져 이름을 찾을 수 없다.
    private static final String WITHDRAWN_USER_NAME = "(탈퇴한 사용자)";

    public record ReceivedRequest(

        @Schema(description = "지목한 학생 학번", example = "202012345", requiredMode = REQUIRED)
        String requesterUserId,

        @Schema(description = "지목한 학생 이름", example = "김철수", requiredMode = REQUIRED)
        String requesterName,

        @Schema(description = "지목 상태(PENDING/ACCEPTED/REJECTED)", example = "PENDING", requiredMode = REQUIRED)
        PreferredPeerStatus status
    ) {
    }

    public static PreSurveyPreferredPeerRequestListResponse from(List<PreSurveyResponse> requests, List<User> users) {
        Map<String, String> names = users.stream().collect(toMap(User::getStudentNumber, User::getName));
        return PreSurveyPreferredPeerRequestListResponse.builder()
            .contents(requests.stream()
                .map(request -> new ReceivedRequest(
                    request.getUserId(),
                    names.getOrDefault(request.getUserId(), WITHDRAWN_USER_NAME),
                    request.getPreferredPeerStatus()))
                .toList())
            .build();
    }
}
