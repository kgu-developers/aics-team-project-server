package importBatch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.importBatch.domain.Status;
import kgu.developers.domain.importBatch.domain.Type;
import kgu.developers.domain.importBatch.exception.ImportBatchAlreadyAppliedException;
import kgu.developers.domain.importBatch.exception.ImportBatchExpiredException;
import kgu.developers.domain.importBatch.exception.ImportBatchHasInvalidRowsException;

class ImportBatchTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 18, 12, 0);
	private static final JsonNode PAYLOAD = JsonConverter.parse("""
			{"headers":["학번","이름"],"rows":[{"rowNumber":2,"cells":["202012345","홍길동"]}]}""");
	private static final JsonNode CLEAN_SUMMARY = JsonConverter.parse("{\"total\":1,\"invalid\":0}");
	private static final JsonNode DIRTY_SUMMARY = JsonConverter.parse("{\"total\":2,\"invalid\":1}");

	static ImportBatch batch(JsonNode summary, LocalDateTime expiredAt) {
		return ImportBatch.create("202012345", 1L, Type.ENROLLMENT, PAYLOAD, summary, expiredAt);
	}

	@Test
	@DisplayName("create는 PREVIEW 상태로 생성하고 payload를 그대로 보관한다")
	void create() {
		ImportBatch batch = batch(CLEAN_SUMMARY, NOW.plusDays(1));

		assertThat(batch.getUploadedBy()).isEqualTo("202012345");
		assertThat(batch.getSectionId()).isEqualTo(1L);
		assertThat(batch.getType()).isEqualTo(Type.ENROLLMENT);
		assertThat(batch.getStatus()).isEqualTo(Status.PREVIEW);
		assertThat(batch.getPayload()).isEqualTo(PAYLOAD);
		assertThat(batch.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("payload는 형식 제약이 없고 값의 타입도 보존된다")
	void anyShapeIsAccepted() {
		JsonNode alien = JsonConverter.parse("""
				{"metadata":{"sheetName":"Employee_Directory_2026"},
				 "data":[{"empId":"EMP-001","salary":75000000,"isActive":true}]}""");

		ImportBatch batch = ImportBatch.create("202012345", 1L, Type.ENROLLMENT,
				alien, JsonConverter.parse("{\"totalRows\":1}"), NOW.plusDays(1));

		assertThat(batch.getPayload().at("/data/0/empId").asText()).isEqualTo("EMP-001");
		assertThat(batch.getPayload().at("/data/0/salary").asLong()).isEqualTo(75000000L);
		assertThat(batch.getPayload().at("/data/0/isActive").asBoolean()).isTrue();
		assertThat(batch.getPayload().at("/metadata/sheetName").asText())
				.isEqualTo("Employee_Directory_2026");
	}

	@Test
	@DisplayName("hasErrors는 summary.invalid로 판단하고, 없으면 오류 없음으로 본다")
	void hasErrors() {
		assertThat(batch(DIRTY_SUMMARY, NOW.plusDays(1)).hasErrors()).isTrue();
		assertThat(batch(CLEAN_SUMMARY, NOW.plusDays(1)).hasErrors()).isFalse();
		assertThat(batch(JsonConverter.parse("{}"), NOW.plusDays(1)).hasErrors()).isFalse();
	}

	@Test
	@DisplayName("create는 NOT NULL 컬럼에 해당하는 인자가 null이면 즉시 거부한다")
	void createRejectsNulls() {
		assertThatThrownBy(() -> batch(CLEAN_SUMMARY, null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("expiredAt");
		assertThatThrownBy(() -> ImportBatch.create("202012345", 1L, Type.ENROLLMENT,
				PAYLOAD, null, NOW))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("summary");
	}

	@Test
	@DisplayName("isExpired는 만료시각이 지났거나 상태가 EXPIRED이면 true를 반환한다")
	void isExpired() {
		assertThat(batch(CLEAN_SUMMARY, NOW.plusDays(1)).isExpired(NOW)).isFalse();
		assertThat(batch(CLEAN_SUMMARY, NOW.minusSeconds(1)).isExpired(NOW)).isTrue();

		ImportBatch expired = batch(CLEAN_SUMMARY, NOW.plusDays(1));
		expired.expire();
		assertThat(expired.isExpired(NOW)).isTrue();
	}

	@Test
	@DisplayName("apply는 오류 없는 미리보기를 APPLIED로 바꾸고, delete는 삭제 시각을 기록한다")
	void applyAndDelete() {
		ImportBatch batch = batch(CLEAN_SUMMARY, NOW.plusDays(1));

		batch.apply(NOW);
		assertThat(batch.getStatus()).isEqualTo(Status.APPLIED);

		batch.delete(NOW);
		assertThat(batch.getDeletedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("apply는 이미 적용됐거나 만료됐거나 오류 행이 있으면 거부한다")
	void applyRejects() {
		ImportBatch applied = batch(CLEAN_SUMMARY, NOW.plusDays(1));
		applied.apply(NOW);
		assertThatThrownBy(() -> applied.apply(NOW))
				.isInstanceOf(ImportBatchAlreadyAppliedException.class);

		ImportBatch expired = batch(CLEAN_SUMMARY, NOW.minusSeconds(1));
		assertThatThrownBy(() -> expired.apply(NOW))
				.isInstanceOf(ImportBatchExpiredException.class);
		assertThat(expired.getStatus()).isEqualTo(Status.PREVIEW);

		ImportBatch invalid = batch(DIRTY_SUMMARY, NOW.plusDays(1));
		assertThatThrownBy(() -> invalid.apply(NOW))
				.isInstanceOf(ImportBatchHasInvalidRowsException.class);
		assertThat(invalid.getStatus()).isEqualTo(Status.PREVIEW);
	}
}
