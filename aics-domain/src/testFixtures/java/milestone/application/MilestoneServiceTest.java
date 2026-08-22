package milestone.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import kgu.developers.domain.milestone.application.command.MilestoneCommandService;
import kgu.developers.domain.milestone.application.command.MilestoneWeekNumberChange;
import kgu.developers.domain.milestone.application.query.MilestoneQueryService;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.exception.DuplicateMilestoneWeekException;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.milestone.exception.MilestoneSectionMismatchException;

class MilestoneServiceTest {
    private FakeMilestoneRepository repository;
    private MilestoneCommandService commandService;
    private MilestoneQueryService queryService;

    @BeforeEach
    void setUp() {
        repository = new FakeMilestoneRepository();
        commandService = new MilestoneCommandService(repository);
        queryService = new MilestoneQueryService(repository);
    }

    @Test
    @DisplayName("마일스톤을 생성하고 식별자로 조회할 수 있다")
    void createAndGetMilestone() {
        Long milestoneId = createMilestone(1L, "제안서", 2);

        Milestone milestone = queryService.getMilestone(1L, milestoneId);

        assertThat(milestone.getSectionId()).isEqualTo(1L);
        assertThat(milestone.getTitle()).isEqualTo("제안서");
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.DRAFT);
    }

    @Test
    @DisplayName("같은 분반에 동일한 주차의 마일스톤을 생성할 수 없다")
    void rejectDuplicateWeekNumberOnCreate() {
        createMilestone(1L, "제안서", 2);

        assertThatThrownBy(() -> createMilestone(1L, "중간보고서", 2))
                .isInstanceOf(DuplicateMilestoneWeekException.class);

        assertThat(queryService.getMilestones(1L, null)).hasSize(1);
    }

    @Test
    @DisplayName("동시 생성이 DB 주차 제약과 충돌하면 명확한 도메인 예외로 변환한다")
    void translateConcurrentDuplicateWeekConflict() {
        repository.failNextSaveWithDataIntegrityViolation();

        assertThatThrownBy(() -> createMilestone(1L, "제안서", 2))
                .isInstanceOf(DuplicateMilestoneWeekException.class);
    }

    @Test
    @DisplayName("주차 중복과 무관한 DB 제약 위반은 중복 주차로 오인하지 않는다")
    void preserveUnrelatedDataIntegrityViolation() {
        repository.failNextSaveWithDataIntegrityViolation("uq_milestone_active_section_week_backup");

        assertThatThrownBy(() -> createMilestone(1L, "제안서", 2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("마일스톤 상세 내용과 일정을 수정할 수 있다")
    void updateMilestone() {
        Long milestoneId = createMilestone(1L, "제안서", 2);
        MilestoneSchedule updatedSchedule = new MilestoneSchedule(
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 9, 30, 23, 59),
                null,
                LocalDateTime.of(2026, 10, 1, 23, 59),
                null,
                null
        );

        commandService.updateMilestone(1L, milestoneId, "제안서 수정", "수정된 설명", updatedSchedule);

        Milestone updated = queryService.getMilestone(1L, milestoneId);
        assertThat(updated.getTitle()).isEqualTo("제안서 수정");
        assertThat(updated.getDescription()).isEqualTo("수정된 설명");
        assertThat(updated.getSchedule()).isEqualTo(updatedSchedule);
    }

    @Test
    @DisplayName("평가 기간만 수정하면 기존 제출 일정은 유지된다")
    void updateEvaluationWindow() {
        Long milestoneId = createMilestone(1L, "제안서", 2);
        MilestoneSchedule originalSchedule = queryService.getMilestone(1L, milestoneId).getSchedule();
        LocalDateTime evaluationOpensAt = originalSchedule.dueAt().plusDays(1);
        LocalDateTime evaluationClosesAt = evaluationOpensAt.plusDays(2);

        commandService.updateEvaluationWindow(
                1L,
                milestoneId,
                evaluationOpensAt,
                evaluationClosesAt
        );

        MilestoneSchedule updatedSchedule = queryService.getMilestone(1L, milestoneId).getSchedule();
        assertThat(updatedSchedule.dueAt()).isEqualTo(originalSchedule.dueAt());
        assertThat(updatedSchedule.evaluationOpensAt()).isEqualTo(evaluationOpensAt);
        assertThat(updatedSchedule.evaluationClosesAt()).isEqualTo(evaluationClosesAt);
    }

    @Test
    @DisplayName("다른 분반의 마일스톤 평가 기간은 수정할 수 없다")
    void rejectEvaluationWindowUpdateFromAnotherSection() {
        Long milestoneId = createMilestone(2L, "다른 분반 제안서", 2);

        assertThatThrownBy(() -> commandService.updateEvaluationWindow(
                1L,
                milestoneId,
                LocalDateTime.of(2026, 9, 11, 0, 0),
                LocalDateTime.of(2026, 9, 12, 0, 0)
        ))
                .isInstanceOf(MilestoneSectionMismatchException.class);
    }

    @Test
    @DisplayName("분반별 목록을 주차 순으로 조회하고 공개 상태로 필터링할 수 있다")
    void getMilestonesBySectionAndVisibility() {
        Long second = createMilestone(1L, "중간보고서", 4);
        Long first = createMilestone(1L, "제안서", 2);
        createMilestone(2L, "다른 분반 제안서", 1);
        commandService.changeStatus(1L, first, MilestoneStatus.PUBLISHED);

        List<Milestone> all = queryService.getMilestones(1L, null);
        List<Milestone> published = queryService.getMilestones(1L, MilestoneStatus.PUBLISHED);

        assertThat(all).extracting(Milestone::getId).containsExactly(first, second);
        assertThat(published).extracting(Milestone::getId).containsExactly(first);
    }

    @Test
    @DisplayName("같은 분반의 마일스톤 주차를 일괄 변경할 수 있다")
    void updateWeekNumbers() {
        Long first = createMilestone(1L, "제안서", 2);
        Long second = createMilestone(1L, "중간보고서", 4);
        Long unchanged = createMilestone(1L, "최종보고서", 8);

        commandService.updateWeekNumbers(1L, List.of(
                new MilestoneWeekNumberChange(first, 3),
                new MilestoneWeekNumberChange(second, 6)
        ));

        assertThat(queryService.getMilestone(1L, first).getWeekNumber()).isEqualTo(3);
        assertThat(queryService.getMilestone(1L, second).getWeekNumber()).isEqualTo(6);
        assertThat(repository.lastSavedBatchIds).containsExactly(first, second, unchanged);
    }

    @Test
    @DisplayName("주차 변경 중 DB 중복 제약과 충돌하면 명확한 도메인 예외로 변환한다")
    void translateConcurrentWeekNumberUpdateConflict() {
        Long milestoneId = createMilestone(1L, "제안서", 2);
        repository.failNextSaveWithDataIntegrityViolation();

        assertThatThrownBy(() -> commandService.updateWeekNumbers(1L, List.of(
                new MilestoneWeekNumberChange(milestoneId, 3)
        )))
                .isInstanceOf(DuplicateMilestoneWeekException.class);
    }

    @Test
    @DisplayName("다른 분반의 마일스톤이 섞이면 일괄 변경을 거부하고 기존 주차를 유지한다")
    void rejectWeekNumberChangeFromAnotherSection() {
        Long first = createMilestone(1L, "제안서", 2);
        Long otherSection = createMilestone(2L, "다른 분반 제안서", 3);

        assertThatThrownBy(() -> commandService.updateWeekNumbers(1L, List.of(
                new MilestoneWeekNumberChange(first, 5),
                new MilestoneWeekNumberChange(otherSection, 6)
        )))
                .isInstanceOf(MilestoneSectionMismatchException.class);

        assertThat(queryService.getMilestone(1L, first).getWeekNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 마일스톤이 주차 변경 요청에 두 번 포함되면 거부한다")
    void rejectDuplicateWeekNumberChange() {
        Long milestoneId = createMilestone(1L, "제안서", 2);

        assertThatThrownBy(() -> commandService.updateWeekNumbers(1L, List.of(
                new MilestoneWeekNumberChange(milestoneId, 3),
                new MilestoneWeekNumberChange(milestoneId, 4)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("두 번");

        assertThat(queryService.getMilestone(1L, milestoneId).getWeekNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("여러 마일스톤을 같은 주차로 변경할 수 없다")
    void rejectDuplicateTargetWeekNumbers() {
        Long first = createMilestone(1L, "제안서", 2);
        Long second = createMilestone(1L, "중간보고서", 4);

        assertThatThrownBy(() -> commandService.updateWeekNumbers(1L, List.of(
                new MilestoneWeekNumberChange(first, 5),
                new MilestoneWeekNumberChange(second, 5)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("중복");

        assertThat(queryService.getMilestone(1L, first).getWeekNumber()).isEqualTo(2);
        assertThat(queryService.getMilestone(1L, second).getWeekNumber()).isEqualTo(4);
    }

    @Test
    @DisplayName("변경하지 않는 마일스톤이 사용 중인 주차로 변경할 수 없다")
    void rejectWeekNumberUsedByUnchangedMilestone() {
        Long first = createMilestone(1L, "제안서", 2);
        Long unchanged = createMilestone(1L, "중간보고서", 4);

        assertThatThrownBy(() -> commandService.updateWeekNumbers(1L, List.of(
                new MilestoneWeekNumberChange(first, 4)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("중복");

        assertThat(queryService.getMilestone(1L, first).getWeekNumber()).isEqualTo(2);
        assertThat(queryService.getMilestone(1L, unchanged).getWeekNumber()).isEqualTo(4);
    }

    @Test
    @DisplayName("같은 요청 안에서 두 마일스톤의 주차를 맞바꿀 수 있다")
    void swapWeekNumbers() {
        Long first = createMilestone(1L, "제안서", 2);
        Long second = createMilestone(1L, "중간보고서", 4);

        commandService.updateWeekNumbers(1L, List.of(
                new MilestoneWeekNumberChange(first, 4),
                new MilestoneWeekNumberChange(second, 2)
        ));

        assertThat(queryService.getMilestone(1L, first).getWeekNumber()).isEqualTo(4);
        assertThat(queryService.getMilestone(1L, second).getWeekNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("존재하지 않는 마일스톤을 조회하면 예외가 발생한다")
    void getMissingMilestone() {
        assertThatThrownBy(() -> queryService.getMilestone(1L, 404L))
                .isInstanceOf(MilestoneNotFoundException.class);
    }

    @Test
    @DisplayName("다른 분반의 마일스톤 상세 조회와 변경을 거부한다")
    void rejectMilestoneAccessFromAnotherSection() {
        Long milestoneId = createMilestone(2L, "다른 분반 제안서", 2);

        assertThatThrownBy(() -> queryService.getMilestone(1L, milestoneId))
                .isInstanceOf(MilestoneSectionMismatchException.class);
        assertThatThrownBy(() -> commandService.changeStatus(1L, milestoneId, MilestoneStatus.PUBLISHED))
                .isInstanceOf(MilestoneSectionMismatchException.class);
    }

    @Test
    @DisplayName("마일스톤 식별자는 양수여야 한다")
    void milestoneIdMustBePositive() {
        assertThatThrownBy(() -> queryService.getMilestone(1L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("식별자");
    }

    @Test
    @DisplayName("일정이 없으면 기존 상세 내용을 변경하지 않는다")
    void rejectUpdateWithoutScheduleBeforeMutation() {
        Long milestoneId = createMilestone(1L, "제안서", 2);

        assertThatThrownBy(() -> commandService.updateMilestone(1L, milestoneId, "변경된 제목", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("일정");

        assertThat(queryService.getMilestone(1L, milestoneId).getTitle()).isEqualTo("제안서");
    }

    private Long createMilestone(Long sectionId, String title, int weekNumber) {
        return commandService.createMilestone(sectionId, title, null, weekNumber, schedule());
    }

    private MilestoneSchedule schedule() {
        return new MilestoneSchedule(null, LocalDateTime.of(2026, 9, 10, 23, 59), null, null, null, null);
    }

    private static final class FakeMilestoneRepository implements MilestoneRepository {
        private final AtomicLong sequence = new AtomicLong(1);
        private final Map<Long, Milestone> milestones = new LinkedHashMap<>();
        private List<Long> lastSavedBatchIds = List.of();
        private String nextDataIntegrityViolationMessage;

        @Override
        public Milestone save(Milestone milestone) {
            if (nextDataIntegrityViolationMessage != null) {
                String message = nextDataIntegrityViolationMessage;
                nextDataIntegrityViolationMessage = null;
                ConstraintViolationException constraintViolationException =
                        new ConstraintViolationException(
                                "제약 조건 위반",
                                new SQLException("테스트용 제약 조건 위반"),
                                message
                        );
                throw new DataIntegrityViolationException("마일스톤 저장 실패", constraintViolationException);
            }
            Milestone saved = milestone;
            if (milestone.getId() == null) {
                saved = Milestone.restore(
                        sequence.getAndIncrement(),
                        milestone.getSectionId(),
                        milestone.getTitle(),
                        milestone.getDescription(),
                        milestone.getWeekNumber(),
                        milestone.getStatus(),
                        milestone.getSchedule()
                );
            }
            milestones.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public List<Milestone> saveAll(List<Milestone> milestones) {
            lastSavedBatchIds = milestones.stream().map(Milestone::getId).toList();
            return milestones.stream().map(this::save).toList();
        }

        @Override
        public Optional<Milestone> findById(Long id) {
            return Optional.ofNullable(milestones.get(id));
        }

        @Override
        public Optional<Milestone> findByIdForUpdate(Long id) {
            return findById(id);
        }

        @Override
        public List<Milestone> findAllBySectionIdOrderByWeekNumber(Long sectionId) {
            return sortedMilestones().stream()
                    .filter(milestone -> milestone.belongsToSection(sectionId))
                    .toList();
        }

        @Override
        public List<Milestone> findAllBySectionIdForUpdateOrderByWeekNumber(Long sectionId) {
            return findAllBySectionIdOrderByWeekNumber(sectionId);
        }

        @Override
        public List<Milestone> findAllBySectionIdAndStatusOrderByWeekNumber(
                Long sectionId,
                MilestoneStatus status
        ) {
            return sortedMilestones().stream()
                    .filter(milestone -> milestone.belongsToSection(sectionId))
                    .filter(milestone -> milestone.getStatus() == status)
                    .toList();
        }

        private List<Milestone> sortedMilestones() {
            List<Milestone> sorted = new ArrayList<>(milestones.values());
            sorted.sort(Comparator.comparingInt(Milestone::getWeekNumber).thenComparing(Milestone::getId));
            return sorted;
        }

        private void failNextSaveWithDataIntegrityViolation() {
            failNextSaveWithDataIntegrityViolation("uq_milestone_active_section_week");
        }

        private void failNextSaveWithDataIntegrityViolation(String message) {
            nextDataIntegrityViolationMessage = message;
        }
    }
}
