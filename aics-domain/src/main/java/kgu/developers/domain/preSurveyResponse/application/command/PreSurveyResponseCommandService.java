package kgu.developers.domain.preSurveyResponse.application.command;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.exception.EnrollmentNotFoundException;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreSurveyResponseCommandService {
  private final PreSurveyResponseRepository preSurveyResponseRepository;
  private final EnrollmentRepository enrollmentRepository;

  // 같은 사용자·분반 조합의 동시 첫 제출을 막는 DB 유니크 제약(database/pre_survey_response.sql)은
  // Flyway 등으로 자동 적용되지 않아 배포 DB에 실제로 있다는 보장이 없다(sunzx0428 PR #65 리뷰
  // 09-03) — 그 제약에만 기대지 않도록, 이미 존재하는 Enrollment 행을 먼저 잠가(비관적 락) 같은
  // 사용자·분반 조합의 요청을 하나씩 직렬화한다. 존재확인→생성 사이의 경쟁상태가 이 잠금 구간
  // 안에서만 일어나므로 DB 제약 존재 여부와 무관하게 중복 행이 생기지 않는다. 아래 유니크 제약
  // catch는 그 제약이 실제로 있는 배포 환경에서의 방어선으로 남겨둔다.
  @Transactional
  public PreSurveyResponse submit(String userId, Long sectionId, JsonNode preferredRoles,
      String topicOpinion, String etcOpinion) {
    enrollmentRepository.findBySectionIdAndUserIdForUpdate(sectionId, userId)
        .orElseThrow(EnrollmentNotFoundException::new);

    PreSurveyResponse existing = preSurveyResponseRepository.findByUserIdAndSectionId(userId, sectionId)
        .orElse(null);

    if (existing != null) {
      existing.update(preferredRoles, topicOpinion, etcOpinion);
      return preSurveyResponseRepository.save(existing);
    }

    try {
      PreSurveyResponse response = PreSurveyResponse.create(userId, sectionId, preferredRoles, topicOpinion, etcOpinion);
      return preSurveyResponseRepository.save(response);
    } catch (DataIntegrityViolationException e) {
      PreSurveyResponse response = preSurveyResponseRepository.findByUserIdAndSectionId(userId, sectionId)
          .orElseThrow(() -> e);
      response.update(preferredRoles, topicOpinion, etcOpinion);
      return preSurveyResponseRepository.save(response);
    }
  }
}
