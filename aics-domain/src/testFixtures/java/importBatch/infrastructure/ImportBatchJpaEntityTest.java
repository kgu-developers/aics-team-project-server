package importBatch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.domain.importBatch.domain.ImportRow;
import kgu.developers.domain.importBatch.domain.ImportPayload;
import kgu.developers.domain.importBatch.domain.ImportSummary;
import kgu.developers.domain.importBatch.domain.RowAction;
import kgu.developers.domain.importBatch.domain.Status;
import kgu.developers.domain.importBatch.domain.Type;
import kgu.developers.domain.importBatch.infrastructure.ImportBatchJpaEntity;

class ImportBatchJpaEntityTest {

	@Test
	@DisplayName("payload/summary는 JSON 문자열로 저장되고 그대로 복원된다")
	void jsonRoundTrip() {
		ImportPayload payload = ImportPayload.of(List.of("학번", "이름"), List.of(
				ImportRow.of(2, List.of("202012345", "홍길동"), RowAction.NEW),
				ImportRow.of(3, List.of("", "이영희"), RowAction.NEW)
						.withErrors(List.of("학번이 비어 있습니다"))
		));
		ImportBatch origin = ImportBatch.create("202012345", 1L, Type.ENROLLMENT, payload,
				LocalDateTime.of(2026, 8, 19, 12, 0));

		ImportBatchJpaEntity entity = ImportBatchJpaEntity.toEntity(origin);
		assertThat(entity.getPayload())
				.startsWith("{\"headers\":[\"학번\",\"이름\"]")
				.contains("\"cells\":[\"202012345\",\"홍길동\"]", "\"errors\":[\"학번이 비어 있습니다\"]")
				.doesNotContain("\"valid\"", "\"applicable\"");
		assertThat(entity.getSummary())
				.isEqualTo("{\"total\":2,\"created\":1,\"updated\":0,\"skipped\":0,\"invalid\":1}");

		ImportBatch restored = entity.toDomain();
		assertThat(restored.getPayload()).isEqualTo(origin.getPayload());
		assertThat(restored.getSummary()).isEqualTo(origin.getSummary());
		assertThat(restored.cell(restored.getPayload().rows().get(0), "이름")).isEqualTo("홍길동");
		assertThat(restored.getExpiredAt()).isEqualTo(origin.getExpiredAt());
	}

	@Test
	@DisplayName("version은 도메인과 엔티티 사이를 왕복한다 (detached merge에서 락이 걸리려면 필수)")
	void versionRoundTrip() {
		ImportPayload payload = ImportPayload.of(List.of("학번"),
				List.of(ImportRow.of(2, List.of("202012345"), RowAction.NEW)));
		ImportBatch stored = ImportBatch.builder()
				.id(1L)
				.version(3L)
				.uploadedBy("202012345")
				.sectionId(1L)
				.type(Type.ENROLLMENT)
				.status(Status.PREVIEW)
				.payload(payload)
				.summary(ImportSummary.of(payload))
				.expiredAt(LocalDateTime.of(2026, 8, 19, 12, 0))
				.build();

		assertThat(ImportBatchJpaEntity.toEntity(stored).getVersion()).isEqualTo(3L);
		assertThat(ImportBatchJpaEntity.toEntity(stored).toDomain().getVersion()).isEqualTo(3L);
	}
}
