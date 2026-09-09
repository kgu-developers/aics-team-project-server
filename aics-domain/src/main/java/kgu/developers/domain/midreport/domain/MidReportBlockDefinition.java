package kgu.developers.domain.midreport.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.midreport.exception.InvalidMidReportFieldsException;
import kgu.developers.domain.midreport.exception.MidReportBlockIncompleteException;

public enum MidReportBlockDefinition {
    TOPIC("topic", "1. 주제", "제안서에서 확정된 주제와 현재 기획 방향을 정리합니다.", List.of(
        new Field("title", "프로젝트 제목", false),
        new Field("description", "주제 설명", true)
    )),
    GUI_DESIGN("gui-design", "2. 화면 GUI 설계", "화면별 이름과 동작 설명을 한 세트씩 등록합니다.", List.of(
        new Field("guiScreens", "화면 GUI 목록", false)
    )),
    ENGINE_DESIGN("engine-design", "3. 엔진부 설계", "클래스 구조와 기능 로직, 실행 관련 파일을 정리합니다.", List.of(
        new Field("features", "구현된 기능 목록", true),
        new Field("architecture", "클래스 구조와 주요 기능 설명", true),
        new Field("testCases", "입력·출력 테스트 케이스", false)
    )),
    PROJECT_PLAN("project-plan", "4. 팀프로젝트 진행 계획", "완료·진행·미구현 항목과 이후 일정을 정리합니다.", List.of(
        new Field("completed", "완료된 내용", false),
        new Field("inProgress", "진행 중인 내용", false),
        new Field("remaining", "미구현 내용", false),
        new Field("help", "문제점 또는 지원 필요", true)
    ));

    private final String key;
    private final String title;
    private final String description;
    private final List<Field> fields;

    MidReportBlockDefinition(String key, String title, String description, List<Field> fields) {
        this.key = key;
        this.title = title;
        this.description = description;
        this.fields = fields;
    }

    public String key() {
        return key;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public static MidReportBlockDefinition fromKey(String key) {
        for (MidReportBlockDefinition definition : values()) {
            if (definition.key.equals(key)) {
                return definition;
            }
        }
        throw new kgu.developers.domain.midreport.exception.MidReportBlockNotFoundException();
    }

    public JsonNode emptyFields(Map<String, String> initialValues) {
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        for (Field field : fields) {
            ObjectNode item = result.addObject();
            item.put("key", field.key());
            item.put("label", field.label());
            item.put("value", initialValues.getOrDefault(field.key(), ""));
            if (field.multiline()) {
                item.put("multiline", true);
            }
        }
        return result;
    }

    public JsonNode normalize(JsonNode requestedFields) {
        if (requestedFields == null || !requestedFields.isArray()) {
            throw new InvalidMidReportFieldsException();
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (JsonNode requested : requestedFields) {
            if (!requested.isObject() || !requested.path("key").isTextual() || !requested.path("value").isTextual()) {
                throw new InvalidMidReportFieldsException();
            }
            if (values.put(requested.path("key").asText(), requested.path("value").asText()) != null) {
                throw new InvalidMidReportFieldsException();
            }
        }
        Set<String> expectedKeys = fields.stream().map(Field::key).collect(Collectors.toSet());
        if (!values.keySet().equals(expectedKeys)) {
            throw new InvalidMidReportFieldsException();
        }
        validateStructuredJson(values);
        return emptyFields(values);
    }

    public void validateComplete(JsonNode currentFields) {
        JsonNode normalized;
        try {
            normalized = normalize(currentFields);
        } catch (InvalidMidReportFieldsException exception) {
            throw new MidReportBlockIncompleteException();
        }
        Map<String, String> values = new LinkedHashMap<>();
        normalized.forEach(field -> values.put(field.path("key").asText(), field.path("value").asText()));
        if (values.values().stream().anyMatch(String::isBlank)) {
            throw new MidReportBlockIncompleteException();
        }
        if (this == GUI_DESIGN) {
            validateRows(values.get("guiScreens"), List.of("id", "name", "description"));
        }
        if (this == ENGINE_DESIGN) {
            validateRows(values.get("testCases"), List.of("id", "description", "input", "output"));
        }
    }

    private void validateStructuredJson(Map<String, String> values) {
        if (this == GUI_DESIGN && !values.get("guiScreens").isBlank()) {
            parseArray(values.get("guiScreens"));
        }
        if (this == ENGINE_DESIGN && !values.get("testCases").isBlank()) {
            parseArray(values.get("testCases"));
        }
    }

    private void validateRows(String json, List<String> requiredKeys) {
        JsonNode rows = parseArray(json);
        if (rows.isEmpty()) {
            throw new MidReportBlockIncompleteException();
        }
        for (JsonNode row : rows) {
            if (!row.isObject() || requiredKeys.stream().anyMatch(key -> !row.path(key).isTextual() || row.path(key).asText().isBlank())) {
                throw new MidReportBlockIncompleteException();
            }
        }
    }

    private JsonNode parseArray(String json) {
        try {
            JsonNode parsed = JsonConverter.parse(json == null ? "" : json);
            if (!parsed.isArray()) {
                throw new InvalidMidReportFieldsException();
            }
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw new InvalidMidReportFieldsException();
        }
    }

    private record Field(String key, String label, boolean multiline) {
    }
}
