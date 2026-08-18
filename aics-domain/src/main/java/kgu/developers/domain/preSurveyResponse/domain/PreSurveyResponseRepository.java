package kgu.developers.domain.preSurveyResponse.domain;

import java.util.List;
import java.util.Optional;

public interface PreSurveyResponseRepository {
	PreSurveyResponse save(PreSurveyResponse response);

	Optional<PreSurveyResponse> findById(Long id);

	Optional<PreSurveyResponse> findByUserIdAndSectionId(String userId, Long sectionId);

	List<PreSurveyResponse> findAllBySectionId(Long sectionId);
}
