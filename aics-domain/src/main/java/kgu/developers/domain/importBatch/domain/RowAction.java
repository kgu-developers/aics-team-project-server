package kgu.developers.domain.importBatch.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RowAction {
	NEW("신규"),
	UPDATE("변경"),
	SKIP("건너뜀");

	private final String description;
}
