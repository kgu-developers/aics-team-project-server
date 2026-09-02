package mock.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.TargetType;

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
		AuditLog log = AuditLog.create("202012345", 1L, "CREATE", TargetType.TEAM, 100L, metadata);

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
		AuditLog log1 = fakeAuditLogRepository.save(AuditLog.create("202012345", 1L, "CREATE", TargetType.TEAM, 100L, metadata));

		fakeAuditLogRepository.save(AuditLog.create("202012345", 2L, "UPDATE", TargetType.TEAM, 101L, metadata));
		fakeAuditLogRepository.save(AuditLog.create("202099999", 1L, "CREATE", TargetType.TEAM, 102L, metadata));

		Pageable pageable = PageRequest.of(0, 10);
		assertThat(fakeAuditLogRepository.findAllByActorId("202012345", pageable).getTotalElements()).isEqualTo(2);
		assertThat(fakeAuditLogRepository.findAllBySectionId(1L, pageable).getTotalElements()).isEqualTo(2);
		assertThat(fakeAuditLogRepository.findAllByEventType("CREATE", pageable).getTotalElements()).isEqualTo(2);

		log1.delete();
		fakeAuditLogRepository.save(log1);

		assertThat(fakeAuditLogRepository.findById(log1.getId())).isEmpty();
		assertThat(fakeAuditLogRepository.findAllByActorId("202012345", pageable).getTotalElements()).isEqualTo(1);
	}
}
