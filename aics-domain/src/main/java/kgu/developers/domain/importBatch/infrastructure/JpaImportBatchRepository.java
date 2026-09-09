package kgu.developers.domain.importBatch.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kgu.developers.domain.importBatch.domain.Status;
import kgu.developers.domain.importBatch.domain.Type;

public interface JpaImportBatchRepository extends JpaRepository<ImportBatchJpaEntity, Long> {
	Optional<ImportBatchJpaEntity> findByIdAndDeletedAtIsNull(Long id);

	List<ImportBatchJpaEntity> findAllBySectionIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long sectionId);

	Optional<ImportBatchJpaEntity> findFirstBySectionIdAndTypeAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(
			Long sectionId, Type type, Status status);
}
