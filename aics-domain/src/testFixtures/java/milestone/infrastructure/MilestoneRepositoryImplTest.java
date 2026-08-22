package milestone.infrastructure;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.milestone.infrastructure.JpaMilestoneRepository;
import kgu.developers.domain.milestone.infrastructure.MilestoneJpaEntity;
import kgu.developers.domain.milestone.infrastructure.MilestoneRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;

@ExtendWith(MockitoExtension.class)
class MilestoneRepositoryImplTest {

    @Mock
    private JpaMilestoneRepository jpaMilestoneRepository;

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
        MilestoneRepositoryImpl repository = new MilestoneRepositoryImpl(jpaMilestoneRepository);

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
        MilestoneRepositoryImpl repository = new MilestoneRepositoryImpl(jpaMilestoneRepository);

        List<Milestone> result = repository.findAllBySectionIdOrderByWeekNumber(7L);

        assertThat(result).extracting(Milestone::getId).containsExactly(1L, 2L);
        verify(jpaMilestoneRepository)
                .findAllBySectionIdAndDeletedAtIsNullOrderByWeekNumberAsc(7L);
    }

    @Test
    @DisplayName("분반 주차 변경용 조회는 활성 마일스톤 전체를 잠가 반환한다")
    void findsAndLocksMilestonesBySectionInWeekOrder() {
        Milestone first = milestone(1L, 1);
        Milestone second = milestone(2L, 2);
        given(jpaMilestoneRepository.findAllActiveBySectionIdForUpdate(7L))
                .willReturn(List.of(
                        MilestoneJpaEntity.fromDomain(first),
                        MilestoneJpaEntity.fromDomain(second)
                ));
        MilestoneRepositoryImpl repository = new MilestoneRepositoryImpl(jpaMilestoneRepository);

        List<Milestone> result = repository
                .findAllBySectionIdForUpdateOrderByWeekNumber(7L);

        assertThat(result).extracting(Milestone::getId).containsExactly(1L, 2L);
        verify(jpaMilestoneRepository).findAllActiveBySectionIdForUpdate(7L);
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
        MilestoneRepositoryImpl repository = new MilestoneRepositoryImpl(jpaMilestoneRepository);

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
    @DisplayName("잠금 조회로 관리 중인 기존 엔티티를 추가 잠금 없이 갱신한다")
    void updatesManagedEntityWithoutSecondLock() {
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
        given(jpaMilestoneRepository.findById(1L))
                .willReturn(Optional.of(existingEntity));
        given(jpaMilestoneRepository.save(existingEntity)).willReturn(existingEntity);
        MilestoneRepositoryImpl repository = new MilestoneRepositoryImpl(jpaMilestoneRepository);

        Milestone result = repository.save(updated);

        assertThat(result.getTitle()).isEqualTo("수정된 마일스톤");
        verify(jpaMilestoneRepository).findById(1L);
        verify(jpaMilestoneRepository).save(existingEntity);
        verify(jpaMilestoneRepository, never()).findActiveByIdForUpdate(anyLong());
    }

    @Test
    @DisplayName("삭제되었거나 존재하지 않는 마일스톤은 저장으로 되살리지 않는다")
    void doesNotRecreateMissingMilestoneDuringUpdate() {
        given(jpaMilestoneRepository.findById(1L))
                .willReturn(Optional.empty());
        MilestoneRepositoryImpl repository = new MilestoneRepositoryImpl(jpaMilestoneRepository);

        assertThatThrownBy(() -> repository.save(milestone(1L, 1)))
                .isInstanceOf(MilestoneNotFoundException.class)
                .extracting("milestoneId")
                .isEqualTo(1L);
        verify(jpaMilestoneRepository).findById(1L);
        verifyNoMoreInteractions(jpaMilestoneRepository);
    }

    @Test
    @DisplayName("수정용 단건 조회는 활성 마일스톤을 잠가 반환한다")
    void findsAndLocksMilestoneById() {
        given(jpaMilestoneRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(MilestoneJpaEntity.fromDomain(milestone(1L, 1))));
        MilestoneRepositoryImpl repository = new MilestoneRepositoryImpl(jpaMilestoneRepository);

        Optional<Milestone> result = repository.findByIdForUpdate(1L);

        assertThat(result).get().extracting(Milestone::getId).isEqualTo(1L);
        verify(jpaMilestoneRepository).findActiveByIdForUpdate(1L);
    }

    @Test
    @DisplayName("주차 맞교환은 임시 주차를 먼저 반영한 뒤 최종 주차로 저장한다")
    void usesTemporaryWeekNumbersBeforeSavingSwappedWeekNumbers() {
        MilestoneJpaEntity firstEntity = MilestoneJpaEntity.fromDomain(milestone(1L, 2));
        MilestoneJpaEntity secondEntity = MilestoneJpaEntity.fromDomain(milestone(2L, 4));
        MilestoneJpaEntity unchangedEntity = MilestoneJpaEntity.fromDomain(milestone(3L, 8));
        List<Milestone> updates = List.of(
                milestone(1L, 4),
                milestone(2L, 2),
                milestone(3L, 8)
        );
        List<List<Integer>> weekNumbersAtFlush = new ArrayList<>();
        given(jpaMilestoneRepository.findAllActiveByIdInForUpdate(List.of(1L, 2L, 3L)))
                .willReturn(List.of(firstEntity, secondEntity, unchangedEntity));
        doAnswer(invocation -> {
            weekNumbersAtFlush.add(List.of(
                    firstEntity.getWeekNumber(),
                    secondEntity.getWeekNumber(),
                    unchangedEntity.getWeekNumber()
            ));
            return null;
        }).when(jpaMilestoneRepository).flush();
        given(jpaMilestoneRepository.saveAllAndFlush(
                List.of(firstEntity, secondEntity, unchangedEntity)
        )).willReturn(List.of(firstEntity, secondEntity, unchangedEntity));
        MilestoneRepositoryImpl repository = new MilestoneRepositoryImpl(jpaMilestoneRepository);

        List<Milestone> result = repository.saveAll(updates);

        assertThat(weekNumbersAtFlush).containsExactly(List.of(9, 10, 8));
        assertThat(result).extracting(Milestone::getWeekNumber).containsExactly(4, 2, 8);
        verify(jpaMilestoneRepository).findAllActiveByIdInForUpdate(List.of(1L, 2L, 3L));
        verify(jpaMilestoneRepository, never()).findActiveByIdForUpdate(anyLong());
        InOrder saveOrder = inOrder(jpaMilestoneRepository);
        saveOrder.verify(jpaMilestoneRepository).flush();
        saveOrder.verify(jpaMilestoneRepository).saveAllAndFlush(
                List.of(firstEntity, secondEntity, unchangedEntity)
        );
    }

    @Test
    @DisplayName("일괄 저장 중 존재하지 않는 마일스톤은 새 엔티티로 만들지 않는다")
    void doesNotRecreateMissingMilestoneDuringBatchUpdate() {
        MilestoneJpaEntity firstEntity = MilestoneJpaEntity.fromDomain(milestone(1L, 1));
        given(jpaMilestoneRepository.findAllActiveByIdInForUpdate(List.of(1L, 2L)))
                .willReturn(List.of(firstEntity));
        MilestoneRepositoryImpl repository = new MilestoneRepositoryImpl(jpaMilestoneRepository);

        assertThatThrownBy(() -> repository.saveAll(List.of(milestone(1L, 3), milestone(2L, 4))))
                .isInstanceOf(MilestoneNotFoundException.class)
                .extracting("milestoneId")
                .isEqualTo(2L);
        verify(jpaMilestoneRepository).findAllActiveByIdInForUpdate(List.of(1L, 2L));
        verifyNoMoreInteractions(jpaMilestoneRepository);
    }

    @Test
    @DisplayName("저장 전 활성 행 조회는 삭제와의 경쟁을 막는 쓰기 잠금을 사용한다")
    void locksActiveRowsBeforeUpdate() throws NoSuchMethodException {
        Lock singleLock = JpaMilestoneRepository.class
                .getMethod("findActiveByIdForUpdate", Long.class)
                .getAnnotation(Lock.class);
        Lock batchLock = JpaMilestoneRepository.class
                .getMethod("findAllActiveByIdInForUpdate", Collection.class)
                .getAnnotation(Lock.class);
        Lock sectionLock = JpaMilestoneRepository.class
                .getMethod("findAllActiveBySectionIdForUpdate", Long.class)
                .getAnnotation(Lock.class);

        assertThat(singleLock.value()).isEqualTo(PESSIMISTIC_WRITE);
        assertThat(batchLock.value()).isEqualTo(PESSIMISTIC_WRITE);
        assertThat(sectionLock.value()).isEqualTo(PESSIMISTIC_WRITE);
    }

    private Milestone milestone(Long id, int weekNumber) {
        return Milestone.restore(
                id,
                7L,
                "마일스톤 " + weekNumber,
                null,
                weekNumber,
                MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(
                        null,
                        LocalDateTime.of(2026, 8, 10 + weekNumber, 23, 59),
                        null,
                        null,
                        null,
                        null
                )
        );
    }
}
