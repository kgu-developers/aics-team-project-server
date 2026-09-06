package kgu.developers.domain.preSurveyResponse.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.exception.EnrollmentNotFoundException;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponsePreferredPeerInvalidException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreSurveyResponseCommandService {
  private final PreSurveyResponseRepository preSurveyResponseRepository;
  private final EnrollmentRepository enrollmentRepository;

  // 같은 사용자·분반 조합의 동시 제출은 Enrollment 행을 먼저 잠가(비관적 락) 직렬화한다 — DB
  // 유니크 제약(database/pre_survey_response.sql)은 Flyway로 자동 적용되지 않아 배포 DB에 실제로
  // 있다는 보장이 없다(sunzx0428 PR #65 리뷰 09-03). 이 잠금이 이미 요청을 직렬화하므로 유니크
  // 제약 위반을 catch해 재조회·갱신으로 복구하는 경로는 두지 않는다 — 같은 트랜잭션 안에서
  // saveAndFlush 실패 후 재조회하면 rollback-only 상태라 정상 동작을 보장할 수 없다(같은 PR
  // 09-04 리뷰).
  //
  // 지목 취소는 preferredPeerUserId 를 null 로 담아 다시 제출하는 것이다. 별도 취소 API 를 두지 않는다.
  @Transactional
  public PreSurveyResponse submit(String userId, Long sectionId, JsonNode preferredRoles,
      String topicOpinion, String etcOpinion, String preferredPeerUserId) {
    validatePreferredPeer(userId, sectionId, preferredPeerUserId);

    enrollmentRepository.findBySectionIdAndUserIdForUpdate(sectionId, userId)
        .orElseThrow(EnrollmentNotFoundException::new);

    PreSurveyResponse existing = preSurveyResponseRepository.findByUserIdAndSectionId(userId, sectionId)
        .orElse(null);

    if (existing != null) {
      existing.update(preferredRoles, topicOpinion, etcOpinion, preferredPeerUserId);
      return preSurveyResponseRepository.save(existing);
    }

    PreSurveyResponse response = PreSurveyResponse.create(userId, sectionId, preferredRoles, topicOpinion,
        etcOpinion, preferredPeerUserId);
    return preSurveyResponseRepository.save(response);
  }

  private void validatePreferredPeer(String userId, Long sectionId, String preferredPeerUserId) {
    if (preferredPeerUserId == null) {
      return;
    }
    if (preferredPeerUserId.equals(userId)) {
      throw new PreSurveyResponsePreferredPeerInvalidException();
    }
    Enrollment peer = enrollmentRepository.findBySectionIdAndUserId(sectionId, preferredPeerUserId)
        .orElseThrow(PreSurveyResponsePreferredPeerInvalidException::new);
    if (!peer.isActiveStudent()) {
      throw new PreSurveyResponsePreferredPeerInvalidException();
    }
  }
}
