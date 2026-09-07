package preSurveyResponse.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.admin.preSurveyResponse.presentation.response.PreSurveyResponseAdminListResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreferredPeerStatus;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.user.domain.User;

class PreSurveyResponseAdminListResponseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("한 학생의 응답 행이 둘이면 최신(id 가 큰) 행으로 매칭을 판정한다")
    void from_UsesLatestResponseWhenUserHasDuplicateRows() throws Exception {
        // B 의 오래된 행은 A 를 지목했지만, 최신 행은 C 를 지목했다 — A 는 짝사랑이라 mutual 이 아니다.
        PreSurveyResponse a = response(1L, "A", "B", PreferredPeerStatus.PENDING);
        PreSurveyResponse staleB = response(2L, "B", "A", PreferredPeerStatus.PENDING);
        PreSurveyResponse latestB = response(3L, "B", "C", PreferredPeerStatus.PENDING);

        PreSurveyResponseAdminListResponse response = PreSurveyResponseAdminListResponse.from(
                List.of(a, staleB, latestB),
                List.of(User.builder().studentNumber("A").name("김철수").build(),
                        User.builder().studentNumber("B").name("이영희").build(),
                        User.builder().studentNumber("C").name("박민수").build()));

        assertThat(response.contents().get(0).mutual()).isFalse();
    }

    private PreSurveyResponse response(Long id, String userId, String peerUserId, PreferredPeerStatus status)
            throws Exception {
        return PreSurveyResponse.builder()
                .id(id)
                .userId(userId)
                .sectionId(1L)
                .preferredRoles(OBJECT_MAPPER.readTree("[\"BACKEND\"]"))
                .preferredPeerUserId(peerUserId)
                .preferredPeerStatus(status)
                .submittedAt(LocalDateTime.now())
                .build();
    }
}
