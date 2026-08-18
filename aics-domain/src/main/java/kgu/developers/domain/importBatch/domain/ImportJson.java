package kgu.developers.domain.importBatch.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.domain.importBatch.exception.ImportBatchPayloadInvalidException;

public final class ImportJson {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private ImportJson() {
	}

	/** 객체 → JSON 트리. 직렬화 자체가 실패하면 던진다 (버그이지 사용자 입력 문제가 아니다). */
	public static JsonNode toTree(Object value) {
		try {
			return MAPPER.valueToTree(value);
		} catch (IllegalArgumentException e) {
			throw new ImportBatchPayloadInvalidException(e);
		}
	}

	/** JSON 문자열 → 트리. 컬럼에 든 값이 JSON이 아니면 던진다. */
	public static JsonNode parse(String json) {
		try {
			return MAPPER.readTree(json);
		} catch (Exception e) {
			throw new ImportBatchPayloadInvalidException(e);
		}
	}
}
