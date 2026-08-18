package kgu.developers.common.json;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonConverter {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private JsonConverter() {
	}

	public static JsonNode toTree(Object value) {
		return toTree(value, IllegalArgumentException::new);
	}

	public static JsonNode toTree(Object value, Function<Throwable, RuntimeException> onInvalid) {
		try {
			return MAPPER.valueToTree(value);
		} catch (IllegalArgumentException e) {
			throw onInvalid.apply(e);
		}
	}

	public static JsonNode parse(String json) {
		return parse(json, IllegalArgumentException::new);
	}

	public static JsonNode parse(String json, Function<Throwable, RuntimeException> onInvalid) {
		try {
			return MAPPER.readTree(json);
		} catch (Exception e) {
			throw onInvalid.apply(e);
		}
	}
}
