package midreport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kgu.developers.domain.midreport.application.command.MidReportCommandService;
import kgu.developers.domain.midreport.application.command.MidReportRevisionCompletionResult;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MidReportCommandServiceTest {
    private static final Long SECTION_ID = 1L;
    private static final Long TEAM_ID = 1L;
    private static final Long MILESTONE_ID = 2L;
    private static final Long REPORT_ID = 10L;

    @Mock private MidReportRepository midReportRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private MilestoneRepository milestoneRepository;
    @InjectMocks private MidReportCommandService midReportCommandService;

    @BeforeEach
    void setUp() {
        lenient().when(milestoneRepository.findByIdAndSectionIdForUpdate(MILESTONE_ID, SECTION_ID))
            .thenReturn(Optional.of(milestone()));
    }

    @Test
    @DisplayName("최초 조회 생성은 마일스톤을 잠근 뒤 팀 행을 잠그고 기존 문서를 확인한다")
    void getOrCreateLocksMilestoneThenTeamBeforeLookup() {
        LocalDateTime dueDate = LocalDateTime.of(2026, 10, 26, 23, 59);
        Team team = Team.builder().id(TEAM_ID).build();
        MidReport saved = MidReport.builder().id(REPORT_ID).teamId(TEAM_ID).milestoneId(MILESTONE_ID).build();
        given(teamRepository.findByIdForUpdate(TEAM_ID)).willReturn(Optional.of(team));
        given(midReportRepository.findByTeamIdAndMilestoneId(TEAM_ID, MILESTONE_ID)).willReturn(Optional.empty());
        given(midReportRepository.save(org.mockito.ArgumentMatchers.any(MidReport.class))).willReturn(saved);

        assertThat(midReportCommandService.getOrCreate(
            SECTION_ID, TEAM_ID, MILESTONE_ID, "보고서", dueDate, "주제", "설명"
        ).getId()).isEqualTo(REPORT_ID);

        InOrder inOrder = org.mockito.Mockito.inOrder(milestoneRepository, teamRepository, midReportRepository);
        inOrder.verify(milestoneRepository).findByIdAndSectionIdForUpdate(MILESTONE_ID, SECTION_ID);
        inOrder.verify(teamRepository).findByIdForUpdate(TEAM_ID);
        inOrder.verify(midReportRepository).findByTeamIdAndMilestoneId(TEAM_ID, MILESTONE_ID);
        inOrder.verify(midReportRepository).save(org.mockito.ArgumentMatchers.any(MidReport.class));
    }

    @Test
    @DisplayName("기존 중간보고서 조회는 중복 생성하지 않는다")
    void getOrCreateReturnsExistingReport() {
        MidReport existing = MidReport.builder().id(REPORT_ID).teamId(TEAM_ID).milestoneId(MILESTONE_ID).build();
        given(teamRepository.findByIdForUpdate(TEAM_ID)).willReturn(Optional.of(Team.builder().id(TEAM_ID).build()));
        given(midReportRepository.findByTeamIdAndMilestoneId(TEAM_ID, MILESTONE_ID)).willReturn(Optional.of(existing));

        MidReport result = midReportCommandService.getOrCreate(
            SECTION_ID, TEAM_ID, MILESTONE_ID, "보고서", LocalDateTime.now(), "주제", "설명"
        );

        assertThat(result).isSameAs(existing);
        then(midReportRepository).should(org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("최종 제출은 도메인 제출 상태를 저장한다")
    void submitsReport() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 9, 8, 14, 0);
        MidReport report = org.mockito.Mockito.mock(MidReport.class);
        given(report.getMilestoneId()).willReturn(MILESTONE_ID);
        given(midReportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));
        given(midReportRepository.save(report)).willReturn(report);

        assertThat(midReportCommandService.submit(SECTION_ID, REPORT_ID, 3L, "202600001", submittedAt)).isSameAs(report);

        then(report).should().submit(3L, "202600001", submittedAt);
        then(midReportRepository).should().save(report);
    }

    @Test
    @DisplayName("마일스톤이 이미 삭제돼 락을 잡지 못하면 기존 보고서 ID로도 제출할 수 없다")
    void rejectsSubmitWhenMilestoneLockFails() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 9, 8, 14, 0);
        MidReport report = org.mockito.Mockito.mock(MidReport.class);
        given(report.getMilestoneId()).willReturn(MILESTONE_ID);
        given(midReportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));
        given(milestoneRepository.findByIdAndSectionIdForUpdate(MILESTONE_ID, SECTION_ID))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> midReportCommandService.submit(SECTION_ID, REPORT_ID, 3L, "202600001", submittedAt))
            .isInstanceOf(MilestoneNotFoundException.class);

        then(midReportRepository).should(org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("수정 요청은 보고서를 다시 열고 수정 대상 영역을 기록한다")
    void requestsRevision() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 9, 9, 10, 0);
        MidReport report = org.mockito.Mockito.mock(MidReport.class);
        given(report.getMilestoneId()).willReturn(MILESTONE_ID);
        given(midReportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));
        given(midReportRepository.save(report)).willReturn(report);
        given(report.requestRevision(java.util.List.of("topic"), requestedAt)).willReturn(true);

        assertThat(midReportCommandService.requestRevision(SECTION_ID, REPORT_ID, java.util.List.of("topic"), requestedAt))
            .isSameAs(report);

        then(report).should().requestRevision(java.util.List.of("topic"), requestedAt);
        then(midReportRepository).should().save(report);
    }

    @Test
    @DisplayName("이미 열린 중간보고서의 피드백은 버전을 바꾸지 않는다")
    void doesNotSaveWhenRevisionIsAlreadyOpen() {
        MidReport report = MidReport.builder().id(REPORT_ID).milestoneId(MILESTONE_ID)
            .status(kgu.developers.domain.midreport.domain.MidReportStatus.DRAFT)
            .blocks(List.of()).build();
        given(midReportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));

        assertThat(midReportCommandService.requestRevision(SECTION_ID, REPORT_ID, List.of(), LocalDateTime.of(2026, 9, 9, 10, 0)))
            .isSameAs(report);

        then(midReportRepository).should(org.mockito.Mockito.never()).save(report);
    }

    @Test
    @DisplayName("피드백 반영 완료 상태가 변경되면 보고서를 저장한다")
    void completesRevisionAndSaves() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 9, 9, 12, 0);
        MidReport report = org.mockito.Mockito.mock(MidReport.class);
        given(report.getMilestoneId()).willReturn(MILESTONE_ID);
        given(midReportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));
        given(report.completeRevision(3L, "professor-1", completedAt)).willReturn(true);
        given(midReportRepository.save(report)).willReturn(report);

        MidReportRevisionCompletionResult result = midReportCommandService.completeRevision(
            SECTION_ID, REPORT_ID, 3L, "professor-1", completedAt
        );

        assertThat(result.report()).isSameAs(report);
        assertThat(result.completed()).isTrue();

        then(midReportRepository).should().save(report);
    }

    @Test
    @DisplayName("이미 완료된 현재 버전은 다시 저장하지 않는다")
    void doesNotSaveRepeatedRevisionCompletion() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 9, 9, 12, 0);
        MidReport report = org.mockito.Mockito.mock(MidReport.class);
        given(report.getMilestoneId()).willReturn(MILESTONE_ID);
        given(midReportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));
        given(report.completeRevision(3L, "professor-1", completedAt)).willReturn(false);

        MidReportRevisionCompletionResult result = midReportCommandService.completeRevision(
            SECTION_ID, REPORT_ID, 3L, "professor-1", completedAt
        );

        assertThat(result.report()).isSameAs(report);
        assertThat(result.completed()).isFalse();

        then(midReportRepository).should(org.mockito.Mockito.never()).save(report);
    }

    private Milestone milestone() {
        return Milestone.create(
            SECTION_ID, "중간보고서", null, 1,
            new MilestoneSchedule(null, LocalDateTime.of(2026, 9, 10, 23, 59), null, null, null, null)
        );
    }
}
