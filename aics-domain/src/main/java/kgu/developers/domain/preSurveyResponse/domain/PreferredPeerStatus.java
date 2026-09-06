package kgu.developers.domain.preSurveyResponse.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PreferredPeerStatus {
	PENDING("지목함 (상대 응답 대기)"),
	ACCEPTED("상대가 수락"),
	REJECTED("상대가 거절");

	private final String description;
}
