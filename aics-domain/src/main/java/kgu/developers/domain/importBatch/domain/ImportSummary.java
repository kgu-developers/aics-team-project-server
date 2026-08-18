package kgu.developers.domain.importBatch.domain;

import java.util.List;

public record ImportSummary(int total, int created, int updated, int skipped, int invalid) {
	public static ImportSummary of(ImportPayload payload) {
		List<ImportRow> rows = payload.rows();
		return new ImportSummary(
				rows.size(),
				count(rows, RowAction.NEW),
				count(rows, RowAction.UPDATE),
				count(rows, RowAction.SKIP),
				(int)rows.stream().filter(row -> !row.isValid()).count());
	}

	private static int count(List<ImportRow> rows, RowAction action) {
		return (int)rows.stream().filter(row -> row.isValid() && row.action() == action).count();
	}
}
