package importBatch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import kgu.developers.domain.importBatch.exception.ImportBatchAlreadyAppliedException;
import kgu.developers.domain.importBatch.exception.ImportBatchExpiredException;
import kgu.developers.domain.importBatch.exception.ImportBatchHasInvalidRowsException;

class ImportBatchTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 18, 12, 0);
	private static final List<String> HEADERS = List.of("학번", "이름", "역할");

	static ImportPayload payload() {
		return ImportPayload.of(HEADERS, List.of(
				ImportRow.of(2, List.of("202012345", "홍길동", "학생"), RowAction.NEW),
				ImportRow.of(3, List.of("202154321", "김철수", "조교"), RowAction.UPDATE),
				ImportRow.of(4, List.of("202099999", "박영수", "학생"), RowAction.SKIP),
				ImportRow.of(5, List.of("", "이영희", "학생"), RowAction.NEW)
						.withErrors(List.of("학번이 비어 있습니다"))
		));
	}

	static ImportPayload cleanPayload() {
		return ImportPayload.of(HEADERS, payload().rows().stream().filter(ImportRow::isValid).toList());
	}

	static ImportBatch batch(LocalDateTime expiredAt) {
		return ImportBatch.create("202012345", 1L, Type.ENROLLMENT, payload(), expiredAt);
	}

	static ImportBatch cleanBatch(LocalDateTime expiredAt) {
		return ImportBatch.create("202012345", 1L, Type.ENROLLMENT, cleanPayload(), expiredAt);
	}

	@Test
	@DisplayName("create는 PREVIEW 상태로 생성하고 처리 구분별 요약을 계산한다")
	void create() {
		ImportBatch batch = batch(NOW.plusDays(1));

		assertThat(batch.getUploadedBy()).isEqualTo("202012345");
		assertThat(batch.getType()).isEqualTo(Type.ENROLLMENT);
		assertThat(batch.getStatus()).isEqualTo(Status.PREVIEW);
		assertThat(batch.getSummary()).isEqualTo(new ImportSummary(4, 1, 1, 1, 1));
		assertThat(batch.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("오류 행은 반영 대상에서 빠지고, 오류를 지우면 원래 action으로 되돌아온다")
	void errorsAreReversible() {
		ImportRow invalid = ImportRow.of(5, List.of("", "이영희", "학생"), RowAction.UPDATE)
				.withErrors(List.of("학번이 비어 있습니다"));

		assertThat(invalid.isValid()).isFalse();
		assertThat(invalid.isApplicable()).isFalse();
		assertThat(invalid.action()).isEqualTo(RowAction.UPDATE);  // 의도는 보존된다

		ImportRow fixed = invalid.withErrors(List.of());
		assertThat(fixed.isValid()).isTrue();
		assertThat(fixed.isApplicable()).isTrue();
		assertThat(fixed.action()).isEqualTo(RowAction.UPDATE);
	}

	@Test
	@DisplayName("create는 NOT NULL 컬럼에 해당하는 인자가 null이면 즉시 거부한다")
	void createRejectsNulls() {
		assertThatThrownBy(() -> ImportBatch.create("202012345", 1L, Type.ENROLLMENT, payload(), null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("expiredAt");
		assertThatThrownBy(() -> ImportBatch.create(null, 1L, Type.ENROLLMENT, payload(), NOW))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("uploadedBy");
	}

	@Test
	@DisplayName("cell은 헤더 이름으로 값을 찾고, 없는 컬럼이면 null이다")
	void cell() {
		ImportBatch batch = batch(NOW.plusDays(1));
		ImportRow first = batch.getPayload().rows().get(0);

		assertThat(batch.cell(first, "학번")).isEqualTo("202012345");
		assertThat(batch.cell(first, "이름")).isEqualTo("홍길동");
		assertThat(batch.cell(first, "없는컬럼")).isNull();
	}

	@Test
	@DisplayName("셀이 모자란 행은 빈 문자열로 채우고, 헤더보다 많으면 거부한다")
	void normalizeCells() {
		ImportPayload padded = ImportPayload.of(HEADERS,
				List.of(ImportRow.of(2, List.of("202012345"), RowAction.NEW)));
		assertThat(padded.rows().get(0).cells()).containsExactly("202012345", "", "");

		assertThatThrownBy(() -> ImportPayload.of(HEADERS,
				List.of(ImportRow.of(2, List.of("a", "b", "c", "d"), RowAction.NEW))))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("getApplicableRows는 오류 없는 NEW/UPDATE만 반환한다 (SKIP·오류 행 제외)")
	void applicableRows() {
		ImportBatch batch = batch(NOW.plusDays(1));

		assertThat(batch.getApplicableRows())
				.extracting(ImportRow::rowNumber)
				.containsExactly(2, 3);
		assertThat(batch.hasErrors()).isTrue();
		assertThat(cleanBatch(NOW.plusDays(1)).hasErrors()).isFalse();
	}

	@Test
	@DisplayName("isExpired는 만료시각이 지났거나 상태가 EXPIRED이면 true를 반환한다")
	void isExpired() {
		assertThat(batch(NOW.plusDays(1)).isExpired(NOW)).isFalse();
		assertThat(batch(NOW.minusSeconds(1)).isExpired(NOW)).isTrue();

		ImportBatch expired = batch(NOW.plusDays(1));
		expired.expire();
		assertThat(expired.isExpired(NOW)).isTrue();
	}

	@Test
	@DisplayName("apply는 오류 없는 미리보기를 APPLIED로 바꾸고, delete는 삭제 시각을 기록한다")
	void applyAndDelete() {
		ImportBatch batch = cleanBatch(NOW.plusDays(1));

		batch.apply(NOW);
		assertThat(batch.getStatus()).isEqualTo(Status.APPLIED);

		batch.delete(NOW);
		assertThat(batch.getDeletedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("apply는 이미 적용됐거나 만료됐거나 오류 행이 있으면 거부한다")
	void applyRejects() {
		ImportBatch applied = cleanBatch(NOW.plusDays(1));
		applied.apply(NOW);
		assertThatThrownBy(() -> applied.apply(NOW))
				.isInstanceOf(ImportBatchAlreadyAppliedException.class);

		ImportBatch expired = cleanBatch(NOW.minusSeconds(1));
		assertThatThrownBy(() -> expired.apply(NOW))
				.isInstanceOf(ImportBatchExpiredException.class);
		assertThat(expired.getStatus()).isEqualTo(Status.PREVIEW);

		ImportBatch invalid = batch(NOW.plusDays(1));  // 오류 행 1건 포함
		assertThatThrownBy(() -> invalid.apply(NOW))
				.isInstanceOf(ImportBatchHasInvalidRowsException.class);
		assertThat(invalid.getStatus()).isEqualTo(Status.PREVIEW);
	}
}
