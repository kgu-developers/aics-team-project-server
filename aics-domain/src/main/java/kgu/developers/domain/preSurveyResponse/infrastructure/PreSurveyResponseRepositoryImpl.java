package kgu.developers.domain.preSurveyResponse.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PreSurveyResponseRepositoryImpl implements PreSurveyResponseRepository {
	private final JpaPreSurveyResponseRepository jpaPreSurveyResponseRepository;

	@Override
	public PreSurveyResponse save(PreSurveyResponse response) {
		PreSurveyResponseJpaEntity entity = PreSurveyResponseJpaEntity.toEntity(response);
		return jpaPreSurveyResponseRepository.save(entity).toDomain();
	}

	@Override
	public Optional<PreSurveyResponse> findById(Long id) {
		return jpaPreSurveyResponseRepository.findByIdAndDeletedAtIsNull(id)
				.map(PreSurveyResponseJpaEntity::toDomain);
	}

	@Override
	public Optional<PreSurveyResponse> findByUserIdAndSectionId(String userId, Long sectionId) {
		return jpaPreSurveyResponseRepository.findByUserIdAndSectionIdAndDeletedAtIsNull(userId, sectionId)
				.map(PreSurveyResponseJpaEntity::toDomain);
	}

	@Override
	public List<PreSurveyResponse> findAllBySectionId(Long sectionId) {
		return jpaPreSurveyResponseRepository.findAllBySectionIdAndDeletedAtIsNullOrderByUserIdAsc(sectionId)
				.stream()
				.map(PreSurveyResponseJpaEntity::toDomain)
				.toList();
	}
}
