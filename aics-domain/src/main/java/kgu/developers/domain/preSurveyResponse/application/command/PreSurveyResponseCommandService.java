package kgu.developers.domain.preSurveyResponse.application.command;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreSurveyResponseCommandService {
  private final PreSurveyResponseRepository preSurveyResponseRepository;

  public PreSurveyResponse submit(String userId, Long sectionId, JsonNode preferredRoles,
      String topicOpinion, String etcOpinion) {
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
