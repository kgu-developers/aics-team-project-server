package midreport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import kgu.developers.domain.midreport.application.query.MidReportQueryService;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.midreport.exception.MidReportNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MidReportQueryServiceTest {

    @Mock
    private MidReportRepository midReportRepository;

    @InjectMocks
    private MidReportQueryService midReportQueryService;

    @Test
    @DisplayName("ID로 중간보고서를 조회할 수 있다")
    void getById_Success() {
        MidReport report = MidReport.builder().id(1L).teamId(10L).milestoneId(100L).build();
        given(midReportRepository.findById(1L)).willReturn(Optional.of(report));

        MidReport result = midReportQueryService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 중간보고서 조회 시 MidReportNotFoundException이 발생한다")
    void getById_NotFound_ThrowsException() {
        given(midReportRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> midReportQueryService.getById(999L))
            .isInstanceOf(MidReportNotFoundException.class);
    }

    @Test
    @DisplayName("팀 ID와 마일스톤 ID로 중간보고서를 Optional로 조회한다")
    void findByTeamIdAndMilestoneId() {
        MidReport report = MidReport.builder().id(1L).teamId(10L).milestoneId(100L).build();
        given(midReportRepository.findByTeamIdAndMilestoneId(10L, 100L)).willReturn(Optional.of(report));

        Optional<MidReport> result = midReportQueryService.findByTeamIdAndMilestoneId(10L, 100L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("팀 ID 목록과 마일스톤 ID로 중간보고서 목록을 조회한다")
    void findAllByTeamIdInAndMilestoneId() {
        MidReport report = MidReport.builder().id(1L).teamId(10L).milestoneId(100L).build();
        given(midReportRepository.findAllByTeamIdInAndMilestoneId(List.of(10L, 20L), 100L))
            .willReturn(List.of(report));

        List<MidReport> result = midReportQueryService.findAllByTeamIdInAndMilestoneId(List.of(10L, 20L), 100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }
}
