package mock.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.dao.DataIntegrityViolationException;

import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;

public class FakePreSurveyResponseRepository implements PreSurveyResponseRepository {

	private final Map<Long, PreSurveyResponse> store = new ConcurrentHashMap<>();
	private final AtomicLong sequence = new AtomicLong(0);

	@Override
	public PreSurveyResponse save(PreSurveyResponse response) {
		if (response.getId() == null) {
			boolean existsActive = store.values().stream()
					.anyMatch(r -> r.getDeletedAt() == null
							&& r.getUserId().equals(response.getUserId())
							&& r.getSectionId().equals(response.getSectionId()));
			if (existsActive) {
				throw new DataIntegrityViolationException("uk_pre_survey_response_active_user_section 위반: 이미 해당 유저의 활성 응답이 존재함");
			}
		}

		Long id = response.getId() != null ? response.getId() : sequence.incrementAndGet();
		LocalDateTime createdAt = response.getCreatedAt() != null ? response.getCreatedAt() : LocalDateTime.now();

		PreSurveyResponse saved = PreSurveyResponse.builder()
				.id(id)
				.userId(response.getUserId())
				.sectionId(response.getSectionId())
				.preferredRoles(response.getPreferredRoles())
				.topicOpinion(response.getTopicOpinion())
				.etcOpinion(response.getEtcOpinion())
				.submittedAt(response.getSubmittedAt())
				.createdAt(createdAt)
				.updatedAt(LocalDateTime.now())
				.deletedAt(response.getDeletedAt())
				.build();

		store.put(id, saved);
		return saved;
	}

	@Override
	public Optional<PreSurveyResponse> findById(Long id) {
		return Optional.ofNullable(store.get(id)).filter(response -> response.getDeletedAt() == null);
	}

	@Override
	public Optional<PreSurveyResponse> findByUserIdAndSectionId(String userId, Long sectionId) {
		return store.values().stream()
				.filter(response -> response.getDeletedAt() == null)
				.filter(response -> response.getUserId().equals(userId))
				.filter(response -> response.getSectionId().equals(sectionId))
				.sorted(Comparator.comparing(PreSurveyResponse::getId).reversed())
				.findFirst();
	}

	@Override
	public List<PreSurveyResponse> findAllBySectionId(Long sectionId) {
		return store.values().stream()
				.filter(response -> response.getDeletedAt() == null)
				.filter(response -> response.getSectionId().equals(sectionId))
				.sorted(Comparator.comparing(PreSurveyResponse::getUserId))
				.toList();
	}
}
