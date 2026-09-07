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
    // 엑셀 내보내기(PreSurveyResponseExcelWriter)도 같은 문구를 쓰도록 공개한다 — 두 화면의 표기가 갈리면 안 된다.
    public static final String WITHDRAWN_USER_NAME = "(탈퇴한 사용자)";

    public static PreSurveyResponseAdminListResponse from(List<PreSurveyResponse> responses, List<User> users) {
        Map<String, String> names = users.stream()
                .collect(toMap(User::getStudentNumber, User::getName));

        // 매칭 판정에 상대 응답의 지목 대상과 수락·거절 상태가 모두 필요하다. 분반 단위 목록이라 메모리에서 맞춘다.
        Map<String, PreSurveyResponse> responseByUser = responses.stream()
                .collect(toMap(PreSurveyResponse::getUserId, response -> response, (first, second) -> first));

        return PreSurveyResponseAdminListResponse.builder()
                .contents(responses.stream()
                        .map(response -> PreSurveyResponseAdminResponse.from(
                                response,
                                names.getOrDefault(response.getUserId(), WITHDRAWN_USER_NAME),
                                peerName(response, names),
                                isMutual(response, responseByUser)))
                        .toList())
                .build();
    }

    private static String peerName(PreSurveyResponse response, Map<String, String> names) {
        if (response.getPreferredPeerUserId() == null) {
            return null;
        }
        return names.getOrDefault(response.getPreferredPeerUserId(), WITHDRAWN_USER_NAME);
    }

    private static boolean isMutual(PreSurveyResponse response, Map<String, PreSurveyResponse> responseByUser) {
        String peer = response.getPreferredPeerUserId();
        if (peer == null) {
            return false;
        }
        if (response.getPreferredPeerStatus() == PreferredPeerStatus.ACCEPTED) {
            return true;   // 내가 지목한 학생이 수락했다
        }
        if (response.getPreferredPeerStatus() == PreferredPeerStatus.REJECTED) {
            return false;  // 거절당했으면 상대가 나를 지목했더라도 매칭이 아니다
        }
        // 아직 대기 중이면 서로 지목이 매칭 근거인데, 내 지목이 거절됐거나 상대 쪽 지목이 거절됐으면 매칭이 아니다.
        PreSurveyResponse peerResponse = responseByUser.get(peer);
        return peerResponse != null
                && Objects.equals(peerResponse.getPreferredPeerUserId(), response.getUserId())
                && peerResponse.getPreferredPeerStatus() != PreferredPeerStatus.REJECTED
                && response.getPreferredPeerStatus() != PreferredPeerStatus.REJECTED;
    }
}
