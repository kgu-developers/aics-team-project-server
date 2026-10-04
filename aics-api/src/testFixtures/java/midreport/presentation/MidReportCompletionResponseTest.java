package midreport.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;
import kgu.developers.api.midreport.presentation.response.MidReportResponse;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportRevision;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MidReportCompletionResponseTest {
    @Test
    @DisplayName("학생 조회 응답도 교수자의 완료 정보와 원래 제출 정보를 구분해서 반환한다")
    void exposesCompletionWithoutFabricatingResubmission() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 10, 1, 12, 0);
        LocalDateTime completedAt = submittedAt.plusDays(2);
        MidReport report = MidReport.create(10L, 20L, "중간보고서", submittedAt.plusDays(5), "주제", "설명");
        report = MidReport.builder()
            .id(100L).teamId(10L).milestoneId(20L).version(2L)
            .title(report.getTitle()).dueDate(report.getDueDate()).blocks(report.getBlocks())
            .status(MidReportStatus.SUBMITTED).submittedAt(submittedAt).submittedBy("202400001")
            .revision(new MidReportRevision(java.util.List.of("topic"), java.util.List.of(),
                submittedAt.plusHours(1), null, completedAt, "202699999"))
            .build();

        MidReportResponse response = MidReportResponse.from(
            report, report.getDueDate(), "학생", "학생", new java.util.HashMap<>(), Map.of()
        );

        assertThat(response.revision().completedAt()).isEqualTo(completedAt);
        assertThat(response.revision().completedBy()).isEqualTo("202699999");
        assertThat(response.revision().resubmittedAt()).isNull();
        assertThat(response.submittedAt()).isEqualTo(submittedAt);
        assertThat(response.submittedBy()).isEqualTo("202400001");
    }
}
