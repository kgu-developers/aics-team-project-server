package mock.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.AuditLogEventType;
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
		AuditLog log = AuditLog.create("202012345", 1L, AuditLogEventType.TEAM_UPDATED, TargetType.TEAM, 100L, metadata);

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
		AuditLog log1 = fakeAuditLogRepository.save(AuditLog.create("202012345", 1L, AuditLogEventType.TEAM_UPDATED, TargetType.TEAM, 100L, metadata));

		fakeAuditLogRepository.save(AuditLog.create("202012345", 2L, AuditLogEventType.TEAM_RULE_UPDATED, TargetType.TEAM, 101L, metadata));
		fakeAuditLogRepository.save(AuditLog.create("202099999", 1L, AuditLogEventType.TEAM_UPDATED, TargetType.TEAM, 102L, metadata));

		Pageable pageable = PageRequest.of(0, 10);
		assertThat(fakeAuditLogRepository.findAllByActorId("202012345", pageable).getTotalElements()).isEqualTo(2);
		assertThat(fakeAuditLogRepository.findAllBySectionId(1L, pageable).getTotalElements()).isEqualTo(2);
		assertThat(fakeAuditLogRepository.findAllByEventType(AuditLogEventType.TEAM_UPDATED, pageable).getTotalElements()).isEqualTo(2);

		log1.delete();
		fakeAuditLogRepository.save(log1);

		assertThat(fakeAuditLogRepository.findById(log1.getId())).isEmpty();
		assertThat(fakeAuditLogRepository.findAllByActorId("202012345", pageable).getTotalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("findAllByTeam은 Pageable의 정렬 조건을 그대로 적용한다")
	void findAllByTeamAppliesPageableSort() {
		AuditLog newer = fakeAuditLogRepository.save(auditLog(
				"202012345", 1L, 100L, LocalDateTime.of(2026, 9, 2, 12, 0)));
		AuditLog older = fakeAuditLogRepository.save(auditLog(
				"202012346", 1L, 100L, LocalDateTime.of(2026, 9, 1, 12, 0)));
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));

		assertThat(fakeAuditLogRepository.findAllByTeam(1L, 100L, pageable).getContent())
				.extracting(AuditLog::getId)
				.containsExactly(older.getId(), newer.getId());
	}

	@Test
	@DisplayName("findAllByTeamAndActorIdIn은 다른 팀에서 발생한 활동을 제외한다")
	void findAllByTeamAndActorIdInExcludesPreviousTeamActivities() {
		AuditLog currentTeam = fakeAuditLogRepository.save(auditLog(
				"202012345", 1L, 100L, LocalDateTime.of(2026, 9, 1, 12, 0)));
		fakeAuditLogRepository.save(auditLog(
				"202012345", 1L, 200L, LocalDateTime.of(2026, 9, 2, 12, 0)));

		assertThat(fakeAuditLogRepository.findAllByTeamAndActorIdIn(
				1L, 100L, List.of("202012345")))
				.extracting(AuditLog::getId)
				.containsExactly(currentTeam.getId());
	}

	private AuditLog auditLog(String actorId, Long sectionId, Long teamId, LocalDateTime createdAt) {
		return AuditLog.builder()
				.actorId(actorId)
				.sectionId(sectionId)
				.eventType(AuditLogEventType.TEAM_UPDATED)
				.targetType(TargetType.TEAM)
				.targetId(teamId)
				.metadata(JsonConverter.parse("{}"))
				.createdAt(createdAt)
				.build();
	}
}
