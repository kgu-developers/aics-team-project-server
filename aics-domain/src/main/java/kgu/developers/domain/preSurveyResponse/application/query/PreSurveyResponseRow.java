package kgu.developers.domain.preSurveyResponse.application.query;

import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;

/**
 * 분반 사전조사 내보내기 한 줄. 아직 응답하지 않은 수강생은 response가 null이다.
 * 이름은 탈퇴 등으로 사용자 정보가 없으면 null일 수 있다.
 */
public record PreSurveyResponseRow(String userId, String name, PreSurveyResponse response) {
}
