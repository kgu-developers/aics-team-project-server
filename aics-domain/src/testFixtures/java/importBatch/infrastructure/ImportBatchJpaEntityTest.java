package importBatch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.domain.importBatch.domain.ImportJson;
import kgu.developers.domain.importBatch.domain.Status;
import kgu.developers.domain.importBatch.domain.Type;
import kgu.developers.domain.importBatch.infrastructure.ImportBatchJpaEntity;

class ImportBatchJpaEntityTest {

	private static final LocalDateTime EXPIRED_AT = LocalDateTime.of(2026, 8, 19, 12, 0);

	@Test
	@DisplayName("어떤 형식의 JSON이든 저장되고 그대로 복원된다 (한글·숫자·boolean 포함)")
	void jsonRoundTrip() {
		JsonNode payload = ImportJson.parse("""
				{"metadata":{"sheetName":"수강생 명단","totalRows":2},\
				"data":[{"학번":"202012345","이름":"홍길동","salary":75000000,"isActive":true}]}""");
		ImportBatch origin = ImportBatch.create("202012345", 1L, Type.ENROLLMENT,
				payload, ImportJson.parse("{\"total\":2,\"invalid\":0}"), EXPIRED_AT);

		ImportBatchJpaEntity entity = ImportBatchJpaEntity.toEntity(origin);
		assertThat(entity.getPayload()).contains("\"학번\":\"202012345\"", "\"salary\":75000000");

		ImportBatch restored = entity.toDomain();
		assertThat(restored.getPayload()).isEqualTo(origin.getPayload());
		assertThat(restored.getSummary()).isEqualTo(origin.getSummary());
		assertThat(restored.getPayload().at("/data/0/이름").asText()).isEqualTo("홍길동");
		assertThat(restored.getPayload().at("/data/0/salary").asLong()).isEqualTo(75000000L);
		assertThat(restored.getPayload().at("/data/0/isActive").asBoolean()).isTrue();
		assertThat(restored.getExpiredAt()).isEqualTo(origin.getExpiredAt());
	}

	@Test
	@DisplayName("version은 도메인과 엔티티 사이를 왕복한다 (detached merge에서 락이 걸리려면 필수)")
	void versionRoundTrip() {
		ImportBatch stored = ImportBatch.builder()
				.id(1L)
				.version(3L)
				.uploadedBy("202012345")
				.sectionId(1L)
				.type(Type.ENROLLMENT)
				.status(Status.PREVIEW)
				.payload(ImportJson.parse("{}"))
				.summary(ImportJson.parse("{}"))
				.expiredAt(EXPIRED_AT)
				.build();

		assertThat(ImportBatchJpaEntity.toEntity(stored).getVersion()).isEqualTo(3L);
		assertThat(ImportBatchJpaEntity.toEntity(stored).toDomain().getVersion()).isEqualTo(3L);
	}
}
