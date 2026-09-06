package kgu.developers.domain.preSurveyResponse.application.query;

import java.util.List;
import java.util.Optional;

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

	public Optional<PreSurveyResponse> findResponse(String userId, Long sectionId) {
		return preSurveyResponseRepository.findByUserIdAndSectionId(userId, sectionId);
	}

	public PreSurveyResponse getResponse(String userId, Long sectionId) {
		return preSurveyResponseRepository.findByUserIdAndSectionId(userId, sectionId)
				.orElseThrow(PreSurveyResponseNotFoundException::new);
	}

	public List<PreSurveyResponse> getReceivedPreferredPeerRequests(String peerUserId, Long sectionId) {
		return preSurveyResponseRepository.findAllBySectionIdAndPreferredPeerUserId(sectionId, peerUserId);
	}
}
