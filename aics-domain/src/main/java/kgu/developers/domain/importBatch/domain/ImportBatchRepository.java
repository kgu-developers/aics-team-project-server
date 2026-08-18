package kgu.developers.domain.importBatch.domain;

import java.util.List;
import java.util.Optional;

public interface ImportBatchRepository {
	ImportBatch save(ImportBatch importBatch);

	Optional<ImportBatch> findById(Long id);

	List<ImportBatch> findAllBySectionId(Long sectionId);
}
