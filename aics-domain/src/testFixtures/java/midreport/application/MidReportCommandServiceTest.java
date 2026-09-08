package midreport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.Optional;
import kgu.developers.domain.midreport.application.command.MidReportCommandService;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MidReportCommandServiceTest {
    @Mock private MidReportRepository midReportRepository;
    @Mock private TeamRepository teamRepository;
    @InjectMocks private MidReportCommandService midReportCommandService;

    @Test
    @DisplayName("최초 조회 생성은 팀 행을 잠근 뒤 기존 문서를 확인한다")
    void getOrCreateLocksTeamBeforeLookup() {
        LocalDateTime dueDate = LocalDateTime.of(2026, 10, 26, 23, 59);
        Team team = Team.builder().id(1L).build();
        MidReport saved = MidReport.builder().id(10L).teamId(1L).milestoneId(2L).build();
        given(teamRepository.findByIdForUpdate(1L)).willReturn(Optional.of(team));
        given(midReportRepository.findByTeamIdAndMilestoneId(1L, 2L)).willReturn(Optional.empty());
        given(midReportRepository.save(org.mockito.ArgumentMatchers.any(MidReport.class))).willReturn(saved);

        assertThat(midReportCommandService.getOrCreate(
            1L, 2L, "보고서", dueDate, "주제", "설명"
        ).getId()).isEqualTo(10L);

        InOrder inOrder = org.mockito.Mockito.inOrder(teamRepository, midReportRepository);
        inOrder.verify(teamRepository).findByIdForUpdate(1L);
        inOrder.verify(midReportRepository).findByTeamIdAndMilestoneId(1L, 2L);
        inOrder.verify(midReportRepository).save(org.mockito.ArgumentMatchers.any(MidReport.class));
    }

    @Test
    @DisplayName("기존 중간보고서 조회는 중복 생성하지 않는다")
    void getOrCreateReturnsExistingReport() {
        MidReport existing = MidReport.builder().id(10L).teamId(1L).milestoneId(2L).build();
        given(teamRepository.findByIdForUpdate(1L)).willReturn(Optional.of(Team.builder().id(1L).build()));
        given(midReportRepository.findByTeamIdAndMilestoneId(1L, 2L)).willReturn(Optional.of(existing));

        MidReport result = midReportCommandService.getOrCreate(
            1L, 2L, "보고서", LocalDateTime.now(), "주제", "설명"
        );

        assertThat(result).isSameAs(existing);
        then(midReportRepository).should(org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }
}
