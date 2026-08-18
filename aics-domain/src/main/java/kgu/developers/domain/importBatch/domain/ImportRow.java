package kgu.developers.domain.importBatch.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public record ImportRow(int rowNumber, List<String> cells, RowAction action, List<String> errors) {
	public ImportRow {
		cells = cells == null ? List.of() : List.copyOf(cells);
		errors = errors == null ? List.of() : List.copyOf(errors);
		action = action == null ? RowAction.NEW : action;
	}

	public static ImportRow of(int rowNumber, List<String> cells, RowAction action) {
		return new ImportRow(rowNumber, cells, action, List.of());
	}

	public ImportRow withErrors(List<String> errors) {
		return new ImportRow(rowNumber, cells, action, errors);
	}

	@JsonIgnore
	public boolean isValid() {
		return errors.isEmpty();
	}

	@JsonIgnore
	public boolean isApplicable() {
		return isValid() && (action == RowAction.NEW || action == RowAction.UPDATE);
	}
}
