package kgu.developers.domain.preSurveyResponse.application.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponseNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreSurveyResponseQueryService {
	private final PreSurveyResponseRepository preSurveyResponseRepository;

	public PreSurveyResponse getResponse(String userId, Long sectionId) {
		return preSurveyResponseRepository.findByUserIdAndSectionId(userId, sectionId)
				.orElseThrow(PreSurveyResponseNotFoundException::new);
	}
}
