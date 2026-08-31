package milestone.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.exception.MilestoneConcurrentlyModifiedException;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.milestone.infrastructure.JpaMilestoneRepository;
import kgu.developers.domain.milestone.infrastructure.MilestoneJpaEntity;
import kgu.developers.domain.milestone.infrastructure.MilestoneRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class MilestoneRepositoryImplTest {

    @Mock
    private JpaMilestoneRepository jpaMilestoneRepository;

    @Mock
    private EntityManager entityManager;

    @Test
    @DisplayName("새 마일스톤은 DB 제약 위반을 호출 경계에서 확인하도록 즉시 반영한다")
    void flushesNewMilestoneImmediately() {
        Milestone newMilestone = Milestone.create(
                7L,
                "마일스톤 1",
                null,
                1,
                new MilestoneSchedule(
                        null,
                        LocalDateTime.of(2026, 8, 11, 23, 59),
                        null,
                        null,
                        null,
                        null
                )
        );
        MilestoneJpaEntity savedEntity = MilestoneJpaEntity.builder()
                .id(1L)
                .sectionId(7L)
                .title("마일스톤 1")
                .weekNumber(1)
                .status(MilestoneStatus.DRAFT)
                .dueAt(LocalDateTime.of(2026, 8, 11, 23, 59))
                .build();
        given(jpaMilestoneRepository.saveAndFlush(any()))
                .willReturn(savedEntity);
        MilestoneRepositoryImpl repository = repository();

        Milestone result = repository.save(newMilestone);

        assertThat(result.getId()).isEqualTo(1L);
        verify(jpaMilestoneRepository).saveAndFlush(any());
        verify(jpaMilestoneRepository, never()).save(any());
    }

    @Test
    @DisplayName("분반의 마일스톤을 주차순으로 조회해 도메인으로 반환한다")
    void findsMilestonesBySectionInWeekOrder() {
        Milestone first = milestone(1L, 1);
        Milestone second = milestone(2L, 2);
        given(jpaMilestoneRepository
                .findAllBySectionIdAndDeletedAtIsNullOrderByWeekNumberAsc(7L))
                .willReturn(List.of(
                        MilestoneJpaEntity.fromDomain(first),
                        MilestoneJpaEntity.fromDomain(second)
                ));
        MilestoneRepositoryImpl repository = repository();

        List<Milestone> result = repository.findAllBySectionIdOrderByWeekNumber(7L);

        assertThat(result).extracting(Milestone::getId).containsExactly(1L, 2L);
        verify(jpaMilestoneRepository)
                .findAllBySectionIdAndDeletedAtIsNullOrderByWeekNumberAsc(7L);
    }

    @Test
    @DisplayName("공개 상태를 지정하면 분반과 상태 조건을 함께 전달한다")
    void findsMilestonesBySectionAndStatus() {
        given(jpaMilestoneRepository
                .findAllBySectionIdAndStatusAndDeletedAtIsNullOrderByWeekNumberAsc(
                        7L,
                        MilestoneStatus.PUBLISHED
                ))
                .willReturn(List.of(MilestoneJpaEntity.fromDomain(milestone(1L, 1))));
        MilestoneRepositoryImpl repository = repository();

        List<Milestone> result = repository
                .findAllBySectionIdAndStatusOrderByWeekNumber(7L, MilestoneStatus.PUBLISHED);

        assertThat(result).hasSize(1);
        verify(jpaMilestoneRepository)
                .findAllBySectionIdAndStatusAndDeletedAtIsNullOrderByWeekNumberAsc(
                        7L,
                        MilestoneStatus.PUBLISHED
                );
    }

    @Test
    @DisplayName("관리 중인 기존 엔티티를 갱신하고 제약 위반을 즉시 확인한다")
    void updatesManagedEntityAndFlushesImmediately() {
        MilestoneJpaEntity existingEntity = MilestoneJpaEntity.fromDomain(milestone(1L, 1));
        Milestone updated = Milestone.restore(
                1L,
                7L,
                "수정된 마일스톤",
                "수정된 설명",
                3,
                MilestoneStatus.DRAFT,
                new MilestoneSchedule(
                        null,
                        LocalDateTime.of(2026, 8, 20, 23, 59),
                        null,
                        null,
                        null,
                        null
                )
        );
        given(entityManager.find(MilestoneJpaEntity.class, 1L)).willReturn(existingEntity);
        given(jpaMilestoneRepository.saveAndFlush(existingEntity)).willReturn(existingEntity);
        MilestoneRepositoryImpl repository = repository();

        Milestone result = repository.save(updated);

        assertThat(result.getTitle()).isEqualTo("수정된 마일스톤");
        verify(entityManager).find(MilestoneJpaEntity.class, 1L);
        verify(jpaMilestoneRepository).saveAndFlush(existingEntity);
    }

    @Test
    @DisplayName("삭제되었거나 존재하지 않는 마일스톤은 저장으로 되살리지 않는다")
    void doesNotRecreateMissingMilestoneDuringUpdate() {
        MilestoneRepositoryImpl repository = repository();

        assertThatThrownBy(() -> repository.save(milestone(1L, 1)))
                .isInstanceOf(MilestoneNotFoundException.class)
                .extracting("milestoneId")
                .isEqualTo(1L);
        verify(entityManager).find(MilestoneJpaEntity.class, 1L);
        verifyNoMoreInteractions(jpaMilestoneRepository, entityManager);
    }

    @Test
    @DisplayName("분반과 식별자가 모두 일치하는 활성 마일스톤을 조회한다")
    void findsMilestoneByIdAndSectionId() {
        given(jpaMilestoneRepository.findByIdAndSectionIdAndDeletedAtIsNull(1L, 7L))
                .willReturn(Optional.of(MilestoneJpaEntity.fromDomain(milestone(1L, 1))));
        MilestoneRepositoryImpl repository = repository();

        Optional<Milestone> result = repository.findByIdAndSectionId(1L, 7L);

        assertThat(result).get().extracting(Milestone::getId).isEqualTo(1L);
        verify(jpaMilestoneRepository).findByIdAndSectionIdAndDeletedAtIsNull(1L, 7L);
    }

    @Test
    @DisplayName("같은 분반의 활성 마일스톤 주차 존재 여부를 가볍게 조회한다")
    void checksActiveWeekNumberExistence() {
        given(jpaMilestoneRepository.existsBySectionIdAndWeekNumberAndDeletedAtIsNull(7L, 2))
                .willReturn(true);
        MilestoneRepositoryImpl repository = repository();

        boolean exists = repository.existsBySectionIdAndWeekNumber(7L, 2);

        assertThat(exists).isTrue();
        verify(jpaMilestoneRepository)
                .existsBySectionIdAndWeekNumberAndDeletedAtIsNull(7L, 2);
    }

    @Test
    @DisplayName("주차 맞교환은 기존 엔티티를 한 번에 조회하고 변경된 행만 저장한다")
    void savesOnlyChangedRowsForSwappedWeekNumbers() {
        MilestoneJpaEntity firstEntity = MilestoneJpaEntity.fromDomain(milestone(1L, 2));
        MilestoneJpaEntity secondEntity = MilestoneJpaEntity.fromDomain(milestone(2L, 4));
        MilestoneJpaEntity unchangedEntity = MilestoneJpaEntity.fromDomain(milestone(3L, 8));
        List<Milestone> updates = List.of(
                milestone(1L, 4),
                milestone(2L, 2),
                milestone(3L, 8)
        );
        given(jpaMilestoneRepository.countBySectionIdAndDeletedAtIsNull(7L)).willReturn(3L);
        given(jpaMilestoneRepository.findAllById(List.of(1L, 2L, 3L)))
                .willReturn(List.of(firstEntity, secondEntity, unchangedEntity));
        MilestoneRepositoryImpl repository = repository();

        List<Milestone> result = repository.saveAllWeekNumberChanges(7L, updates);

        assertThat(result).extracting(Milestone::getWeekNumber).containsExactly(4, 2, 8);
        verify(jpaMilestoneRepository).findAllById(List.of(1L, 2L, 3L));
        verify(jpaMilestoneRepository).saveAllAndFlush(List.of(firstEntity, secondEntity));
        verify(jpaMilestoneRepository, never()).flush();
    }

    @Test
    @DisplayName("일괄 저장 중 존재하지 않는 마일스톤은 새 엔티티로 만들지 않는다")
    void doesNotRecreateMissingMilestoneDuringBatchUpdate() {
        MilestoneJpaEntity firstEntity = MilestoneJpaEntity.fromDomain(milestone(1L, 1));
        given(jpaMilestoneRepository.countBySectionIdAndDeletedAtIsNull(7L)).willReturn(2L);
        given(jpaMilestoneRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(firstEntity));
        MilestoneRepositoryImpl repository = repository();

        assertThatThrownBy(() -> repository.saveAllWeekNumberChanges(
                7L,
                List.of(milestone(1L, 3), milestone(2L, 4))
        ))
                .isInstanceOf(MilestoneNotFoundException.class)
                .extracting("milestoneId")
                .isEqualTo(2L);
        verify(jpaMilestoneRepository).findAllById(List.of(1L, 2L));
        verify(jpaMilestoneRepository).countBySectionIdAndDeletedAtIsNull(7L);
        verifyNoMoreInteractions(jpaMilestoneRepository, entityManager);
    }

    @Test
    @DisplayName("분반의 활성 마일스톤 전체가 아니면 주차 일괄 변경을 거부한다")
    void rejectsIncompleteSectionSnapshot() {
        given(jpaMilestoneRepository.countBySectionIdAndDeletedAtIsNull(7L)).willReturn(3L);
        MilestoneRepositoryImpl repository = repository();

        assertThatThrownBy(() -> repository.saveAllWeekNumberChanges(
                7L,
                List.of(milestone(1L, 2), milestone(2L, 1))
        ))
                .isInstanceOf(MilestoneConcurrentlyModifiedException.class);
        verify(jpaMilestoneRepository).countBySectionIdAndDeletedAtIsNull(7L);
        verifyNoMoreInteractions(jpaMilestoneRepository);
    }

    @Test
    @DisplayName("중복 식별자로 일부 마일스톤을 누락한 일괄 변경을 거부한다")
    void rejectsDuplicateIdsInSectionSnapshot() {
        given(jpaMilestoneRepository.countBySectionIdAndDeletedAtIsNull(7L)).willReturn(2L);
        MilestoneRepositoryImpl repository = repository();

        assertThatThrownBy(() -> repository.saveAllWeekNumberChanges(
                7L,
                List.of(milestone(1L, 2), milestone(1L, 1))
        ))
                .isInstanceOf(MilestoneConcurrentlyModifiedException.class);
        verify(jpaMilestoneRepository).countBySectionIdAndDeletedAtIsNull(7L);
        verifyNoMoreInteractions(jpaMilestoneRepository);
    }

    @Test
    @DisplayName("다른 DB 제약 위반은 원래 예외를 유지한다")
    void preservesUnrelatedConstraintViolation() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("저장 실패");
        given(jpaMilestoneRepository.saveAndFlush(any())).willThrow(violation);
        MilestoneRepositoryImpl repository = repository();

        assertThatThrownBy(() -> repository.save(Milestone.create(
                7L,
                "마일스톤",
                null,
                1,
                schedule(1)
        )))
                .isSameAs(violation);
    }

    private Milestone milestone(Long id, int weekNumber) {
        return Milestone.restore(
                id,
                7L,
                "마일스톤 " + weekNumber,
                null,
                weekNumber,
                MilestoneStatus.PUBLISHED,
                schedule(weekNumber)
        );
    }

    private MilestoneSchedule schedule(int weekNumber) {
        return new MilestoneSchedule(
                null,
                LocalDateTime.of(2026, 8, 10 + weekNumber, 23, 59),
                null,
                null,
                null,
                null
        );
    }

    private MilestoneRepositoryImpl repository() {
        return new MilestoneRepositoryImpl(jpaMilestoneRepository, entityManager);
    }
}
