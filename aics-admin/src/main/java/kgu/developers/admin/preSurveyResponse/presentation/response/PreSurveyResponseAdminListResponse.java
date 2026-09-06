package kgu.developers.admin.preSurveyResponse.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreferredPeerStatus;
import kgu.developers.domain.user.domain.User;

@Builder
public record PreSurveyResponseAdminListResponse(

        @Schema(description = "분반 학생 사전조사 응답 목록", requiredMode = REQUIRED)
        List<PreSurveyResponseAdminResponse> contents
) {

    // 응답을 남긴 뒤 탈퇴한 학생은 user 행이 사라져 이름을 찾을 수 없다. 응답 자체는 그대로 보여줘야 하므로 이름만 대체 문구로 채운다.
    private static final String WITHDRAWN_USER_NAME = "(탈퇴한 사용자)";

    public static PreSurveyResponseAdminListResponse from(List<PreSurveyResponse> responses, List<User> users) {
        Map<String, String> names = users.stream()
                .collect(toMap(User::getStudentNumber, User::getName));

        // 서로 지목: 내가 지목한 학생의 응답이 다시 나를 지목하고 있으면 true. 분반 단위 목록이라 메모리에서 맞춘다.
        Map<String, String> preferredPeerByUser = responses.stream()
                .filter(response -> response.getPreferredPeerUserId() != null)
                .collect(toMap(PreSurveyResponse::getUserId, PreSurveyResponse::getPreferredPeerUserId,
                        (first, second) -> first));

        return PreSurveyResponseAdminListResponse.builder()
                .contents(responses.stream()
                        .map(response -> PreSurveyResponseAdminResponse.from(
                                response,
                                names.getOrDefault(response.getUserId(), WITHDRAWN_USER_NAME),
                                peerName(response, names),
                                isMutual(response, preferredPeerByUser)))
                        .toList())
                .build();
    }

    private static String peerName(PreSurveyResponse response, Map<String, String> names) {
        if (response.getPreferredPeerUserId() == null) {
            return null;
        }
        return names.getOrDefault(response.getPreferredPeerUserId(), WITHDRAWN_USER_NAME);
    }

    private static boolean isMutual(PreSurveyResponse response, Map<String, String> preferredPeerByUser) {
        String peer = response.getPreferredPeerUserId();
        if (peer == null) {
            return false;
        }
        // 서로 지목했거나, 한쪽이 수락한 경우
        boolean mutuallyNominated = Objects.equals(preferredPeerByUser.get(peer), response.getUserId());
        boolean accepted = response.getPreferredPeerStatus() == PreferredPeerStatus.ACCEPTED;
        return mutuallyNominated || accepted;
    }
}
