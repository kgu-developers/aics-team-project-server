package mock.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.auditLog.domain.AuditLog;

class FakeAuditLogRepositoryTest {

	private FakeAuditLogRepository fakeAuditLogRepository;

	@BeforeEach
	void setUp() {
		fakeAuditLogRepository = new FakeAuditLogRepository();
	}

	@Test
	@DisplayName("save 및 findById 동작 검증")
	void saveAndFindById() {
		JsonNode metadata = JsonConverter.parse("{\"key\":\"value\"}");
		AuditLog log = AuditLog.create("202012345", 1L, "CREATE", 10L, 100L, metadata);

		AuditLog saved = fakeAuditLogRepository.save(log);

		assertThat(saved.getId()).isNotNull();
		Optional<AuditLog> found = fakeAuditLogRepository.findById(saved.getId());
		assertThat(found).isPresent();
		assertThat(found.get().getActorId()).isEqualTo("202012345");
	}

	@Test
	@DisplayName("조회 조건별 필터링 및 소프트 삭제된 로그 제외 검증")
	void filteringAndSoftDelete() {
		JsonNode metadata = JsonConverter.parse("{}");
		AuditLog log1 = fakeAuditLogRepository.save(AuditLog.create("202012345", 1L, "CREATE", 10L, 100L, metadata));
		AuditLog log2 = fakeAuditLogRepository.save(AuditLog.create("202012345", 2L, "UPDATE", 10L, 101L, metadata));
		AuditLog log3 = fakeAuditLogRepository.save(AuditLog.create("202099999", 1L, "CREATE", 10L, 102L, metadata));

		assertThat(fakeAuditLogRepository.findAllByActorId("202012345")).hasSize(2);
		assertThat(fakeAuditLogRepository.findAllBySectionId(1L)).hasSize(2);
		assertThat(fakeAuditLogRepository.findAllByEventType("CREATE")).hasSize(2);

		log1.delete();
		fakeAuditLogRepository.save(log1);

		assertThat(fakeAuditLogRepository.findById(log1.getId())).isEmpty();
		assertThat(fakeAuditLogRepository.findAllByActorId("202012345")).hasSize(1);
	}
}
