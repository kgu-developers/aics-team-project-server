package kgu.developers.domain.importBatch.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.domain.importBatch.domain.ImportBatchRepository;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ImportBatchRepositoryImpl implements ImportBatchRepository {
	private final JpaImportBatchRepository jpaImportBatchRepository;
	private final EntityManager entityManager;

	@Override
	public ImportBatch save(ImportBatch importBatch) {
		SectionJpaEntity section = entityManager.getReference(SectionJpaEntity.class, importBatch.getSectionId());
		UserJpaEntity uploadedBy = entityManager.getReference(UserJpaEntity.class, importBatch.getUploadedBy());
		ImportBatchJpaEntity entity = ImportBatchJpaEntity.toEntity(importBatch, section, uploadedBy);
		return jpaImportBatchRepository.save(entity).toDomain();
	}

	@Override
	public Optional<ImportBatch> findById(Long id) {
		return jpaImportBatchRepository.findByIdAndDeletedAtIsNull(id)
				.map(ImportBatchJpaEntity::toDomain);
	}

	@Override
	public List<ImportBatch> findAllBySectionId(Long sectionId) {
		return jpaImportBatchRepository.findAllBySectionIdAndDeletedAtIsNullOrderByCreatedAtDesc(sectionId)
				.stream()
				.map(ImportBatchJpaEntity::toDomain)
				.toList();
	}
}
