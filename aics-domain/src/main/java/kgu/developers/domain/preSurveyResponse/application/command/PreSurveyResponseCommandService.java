package kgu.developers.domain.preSurveyResponse.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PreSurveyResponseCommandService {
  private final PreSurveyResponseRepository preSurveyResponseRepository;

  public PreSurveyResponse submit(String userId, Long sectionId, JsonNode preferredRoles,
      String topicOpinion, String etcOpinion) {
    PreSurveyResponse response = preSurveyResponseRepository.findByUserIdAndSectionId(userId, sectionId)
        .map(existing -> {
          existing.update(preferredRoles, topicOpinion, etcOpinion);
          return existing;
        })
        .orElseGet(() -> PreSurveyResponse.create(userId, sectionId, preferredRoles, topicOpinion, etcOpinion));

    return preSurveyResponseRepository.save(response);
  }
}
