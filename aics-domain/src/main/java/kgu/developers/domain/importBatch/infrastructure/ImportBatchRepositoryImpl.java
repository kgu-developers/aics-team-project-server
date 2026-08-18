package kgu.developers.domain.importBatch.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.domain.importBatch.domain.ImportBatchRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ImportBatchRepositoryImpl implements ImportBatchRepository {
	private final JpaImportBatchRepository jpaImportBatchRepository;

	@Override
	public ImportBatch save(ImportBatch importBatch) {
		ImportBatchJpaEntity entity = ImportBatchJpaEntity.toEntity(importBatch);
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
