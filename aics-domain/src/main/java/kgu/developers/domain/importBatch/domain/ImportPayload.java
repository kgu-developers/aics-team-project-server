package kgu.developers.domain.importBatch.domain;

import java.util.ArrayList;
import java.util.List;

public record ImportPayload(List<String> headers, List<ImportRow> rows) {
	public ImportPayload {
		headers = headers == null ? List.of() : List.copyOf(headers);
		rows = normalize(rows == null ? List.of() : rows, headers.size());
	}

	public static ImportPayload of(List<String> headers, List<ImportRow> rows) {
		return new ImportPayload(headers, rows);
	}

	public String cell(ImportRow row, String column) {
		int index = headers.indexOf(column);
		return index < 0 || index >= row.cells().size() ? null : row.cells().get(index);
	}

	public List<ImportRow> applicableRows() {
		return rows.stream().filter(ImportRow::isApplicable).toList();
	}

	private static List<ImportRow> normalize(List<ImportRow> rows, int width) {
		List<ImportRow> normalized = new ArrayList<>(rows.size());
		for (ImportRow row : rows) {
			int size = row.cells().size();
			if (size > width) {
				throw new IllegalArgumentException(
						"%d행의 셀 수(%d)가 헤더 수(%d)보다 많습니다".formatted(row.rowNumber(), size, width));
			}
			if (size == width) {
				normalized.add(row);
				continue;
			}
			List<String> padded = new ArrayList<>(row.cells());
			while (padded.size() < width) {
				padded.add("");
			}
			normalized.add(new ImportRow(row.rowNumber(), padded, row.action(), row.errors()));
		}
		return List.copyOf(normalized);
	}
}
