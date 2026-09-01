package auditLog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.TargetType;
import kgu.developers.domain.auditLog.exception.AuditLogNotFoundException;
import kgu.developers.domain.auditLog.infrastructure.AuditLogJpaEntity;
import kgu.developers.domain.auditLog.infrastructure.AuditLogRepositoryImpl;
import kgu.developers.domain.auditLog.infrastructure.JpaAuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditLogRepositoryImplTest {

	@Mock
	private JpaAuditLogRepository jpaAuditLogRepository;

	@InjectMocks
	private AuditLogRepositoryImpl auditLogRepositoryImpl;

	private AuditLogJpaEntity sampleEntity() {
		return AuditLogJpaEntity.builder()
				.id(1L)
				.actorId("202012345")
				.sectionId(10L)
				.eventType("CREATE")
				.targetType(TargetType.TEAM.getCode())
				.targetId(5L)
				.metadata("{}")
				.build();
	}

	@Test
	@DisplayName("save는 전달받은 AuditLog를 엔티티로 변환하여 저장 후 도메인 객체로 반환한다")
	void save() {
		AuditLog auditLog = AuditLog.create("202012345", 10L, "CREATE", TargetType.TEAM, 5L, JsonConverter.parse("{}"));
		given(jpaAuditLogRepository.save(any())).willReturn(sampleEntity());

		AuditLog saved = auditLogRepositoryImpl.save(auditLog);

		assertThat(saved.getId()).isEqualTo(1L);
		assertThat(saved.getActorId()).isEqualTo("202012345");
		verify(jpaAuditLogRepository).save(any(AuditLogJpaEntity.class));
	}

	@Test
	@DisplayName("findById는 삭제되지 않은 감사 로그를 조회한다")
	void findById() {
		given(jpaAuditLogRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(sampleEntity()));

		Optional<AuditLog> found = auditLogRepositoryImpl.findById(1L);

		assertThat(found).isPresent();
		assertThat(found.get().getId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("findAllBySectionId는 특정 분반의 감사 로그 목록을 조회한다")
	void findAllBySectionId() {
		Pageable pageable = PageRequest.of(0, 10);
		given(jpaAuditLogRepository.findAllBySectionIdAndDeletedAtIsNull(10L, pageable))
				.willReturn(new PageImpl<>(List.of(sampleEntity()), pageable, 1));

		Page<AuditLog> result = auditLogRepositoryImpl.findAllBySectionId(10L, pageable);

		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).getSectionId()).isEqualTo(10L);
	}

	@Test
	@DisplayName("findAllByActorId는 특정 행위자의 감사 로그 목록을 조회한다")
	void findAllByActorId() {
		Pageable pageable = PageRequest.of(0, 10);
		given(jpaAuditLogRepository.findAllByActorIdAndDeletedAtIsNull("202012345", pageable))
				.willReturn(new PageImpl<>(List.of(sampleEntity()), pageable, 1));

		Page<AuditLog> result = auditLogRepositoryImpl.findAllByActorId("202012345", pageable);

		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).getActorId()).isEqualTo("202012345");
	}

	@Test
	@DisplayName("findAllByEventType은 특정 이벤트 유형의 감사 로그 목록을 조회한다")
	void findAllByEventType() {
		Pageable pageable = PageRequest.of(0, 10);
		given(jpaAuditLogRepository.findAllByEventTypeAndDeletedAtIsNull("CREATE", pageable))
				.willReturn(new PageImpl<>(List.of(sampleEntity()), pageable, 1));

		Page<AuditLog> result = auditLogRepositoryImpl.findAllByEventType("CREATE", pageable);

		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).getEventType()).isEqualTo("CREATE");
	}

	@Test
	@DisplayName("findAllByTeam은 특정 분반과 팀을 대상으로 한 감사 로그만 조회한다")
	void findAllByTeam() {
		Pageable pageable = PageRequest.of(0, 10);
		given(jpaAuditLogRepository.findAllBySectionIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(
				10L, TargetType.TEAM.getCode(), 5L, pageable))
				.willReturn(new PageImpl<>(List.of(sampleEntity()), pageable, 1));

		Page<AuditLog> result = auditLogRepositoryImpl.findAllByTeam(10L, 5L, pageable);

		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).getTargetType()).isEqualTo(TargetType.TEAM);
		assertThat(result.getContent().get(0).getTargetId()).isEqualTo(5L);
	}

	@Test
	@DisplayName("findAllBySectionIdAndActorIdIn은 팀원들의 활동을 최신순으로 조회한다")
	void findAllBySectionIdAndActorIdIn() {
		List<String> actorIds = List.of("202012345", "202012346");
		given(jpaAuditLogRepository
				.findAllBySectionIdAndActorIdInAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(10L, actorIds))
				.willReturn(List.of(sampleEntity()));

		List<AuditLog> result = auditLogRepositoryImpl.findAllBySectionIdAndActorIdIn(10L, actorIds);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getActorId()).isEqualTo("202012345");
	}

	@Test
	@DisplayName("deleteById는 조회한 엔티티에 deleted_at을 채운다")
	void deleteById() {
		AuditLogJpaEntity entity = sampleEntity();
		given(jpaAuditLogRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(entity));

		auditLogRepositoryImpl.deleteById(1L);

		assertThat(entity.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("deleteById는 없는 id면 AuditLogNotFoundException(404)을 던진다")
	void deleteByIdNotFound() {
		assertThatThrownBy(() -> auditLogRepositoryImpl.deleteById(99L))
				.isInstanceOf(AuditLogNotFoundException.class);
		assertThatThrownBy(() -> auditLogRepositoryImpl.deleteById(null))
				.isInstanceOf(AuditLogNotFoundException.class);
	}

	@Test
	@DisplayName("null 또는 빈 인자로 조회 시 빈 결과를 반환하고 DB 조회를 건너뛴다")
	void nullOrBlankArgumentsReturnEmpty() {
		Pageable pageable = PageRequest.of(0, 10);
		assertThat(auditLogRepositoryImpl.findById(null)).isEmpty();
		assertThat(auditLogRepositoryImpl.findAllBySectionId(null, pageable)).isEmpty();
		assertThat(auditLogRepositoryImpl.findAllByActorId(null, pageable)).isEmpty();
		assertThat(auditLogRepositoryImpl.findAllByActorId(" ", pageable)).isEmpty();
		assertThat(auditLogRepositoryImpl.findAllByEventType(null, pageable)).isEmpty();
		assertThat(auditLogRepositoryImpl.findAllByEventType(" ", pageable)).isEmpty();
		assertThat(auditLogRepositoryImpl.findAllByTeam(null, 1L, pageable)).isEmpty();
		assertThat(auditLogRepositoryImpl.findAllByTeam(1L, null, pageable)).isEmpty();
		assertThat(auditLogRepositoryImpl.findAllBySectionIdAndActorIdIn(null, List.of("202012345"))).isEmpty();
		assertThat(auditLogRepositoryImpl.findAllBySectionIdAndActorIdIn(1L, List.of())).isEmpty();
	}
}
