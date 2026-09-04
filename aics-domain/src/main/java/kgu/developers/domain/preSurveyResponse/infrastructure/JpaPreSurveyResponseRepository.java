package kgu.developers.domain.preSurveyResponse.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPreSurveyResponseRepository extends JpaRepository<PreSurveyResponseJpaEntity, Long> {
	Optional<PreSurveyResponseJpaEntity> findByIdAndDeletedAtIsNull(Long id);

	Optional<PreSurveyResponseJpaEntity> findFirstByUserIdAndSectionIdAndDeletedAtIsNullOrderByIdDesc(String userId, Long sectionId);

	List<PreSurveyResponseJpaEntity> findAllBySectionIdAndDeletedAtIsNullOrderByUserIdAsc(Long sectionId);
}
