package midreport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportBlock;
import kgu.developers.domain.midreport.domain.MidReportBlockStatus;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import kgu.developers.domain.midreport.exception.InvalidMidReportFieldsException;
import kgu.developers.domain.midreport.exception.MidReportBlockIncompleteException;
import kgu.developers.domain.midreport.exception.MidReportBlockNotFoundException;
import kgu.developers.domain.midreport.exception.MidReportSubmittedException;
import kgu.developers.domain.midreport.exception.MidReportVersionConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MidReportTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("중간보고서는 고정된 네 영역과 프로젝트 주제 초깃값으로 생성된다")
    void createsFixedBlocks() {
        MidReport report = report(0L, MidReportStatus.DRAFT);

        assertThat(report.getBlocks()).extracting(MidReportBlock::getKey)
            .containsExactly("topic", "gui-design", "engine-design", "project-plan");
        assertThat(report.getBlocks().get(0).getFields().get(0).path("value").asText()).isEqualTo("CineFlow");
    }

    @Test
    @DisplayName("완료된 영역을 저장하면 IN_PROGRESS로 돌아가고 편집 이력을 기록한다")
    void editingCompletedBlockReturnsToInProgress() throws Exception {
        MidReport report = report(0L, MidReportStatus.DRAFT);
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 12, 0);
        report.completeBlock("topic", 0L, "202600001", now.minusMinutes(1));

        report.updateBlock("topic", 0L, topicFields(), "202600002", now);

        MidReportBlock topic = report.getBlocks().get(0);
        assertThat(topic.getStatus()).isEqualTo(MidReportBlockStatus.IN_PROGRESS);
        assertThat(topic.getLastEditedBy()).isEqualTo("202600002");
        assertThat(topic.getLastSavedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("요청 버전이 현재 버전과 다르면 VERSION_CONFLICT를 발생시킨다")
    void rejectsVersionMismatch() {
        MidReport report = report(4L, MidReportStatus.DRAFT);

        assertThatThrownBy(() -> report.updateBlock(
            "topic", 3L, topicFields(), "202600001", LocalDateTime.now()
        )).isInstanceOf(MidReportVersionConflictException.class)
          .extracting("code.code").isEqualTo("VERSION_CONFLICT");
    }

    @Test
    @DisplayName("정의되지 않은 영역 키는 버전 검증보다 먼저 404 오류로 거부한다")
    void rejectsUnknownBlockBeforeVersionCheck() {
        MidReport report = report(4L, MidReportStatus.DRAFT);

        assertThatThrownBy(() -> report.updateBlock(
            "mid-check-question", 3L, topicFields(), "202600001", LocalDateTime.now()
        )).isInstanceOf(MidReportBlockNotFoundException.class);
    }

    @Test
    @DisplayName("제출된 문서는 저장하거나 완료 처리할 수 없다")
    void submittedReportIsReadOnly() {
        MidReport report = report(0L, MidReportStatus.SUBMITTED);

        assertThatThrownBy(() -> report.updateBlock(
            "topic", 0L, topicFields(), "202600001", LocalDateTime.now()
        )).isInstanceOf(MidReportSubmittedException.class);
        assertThatThrownBy(() -> report.completeBlock(
            "topic", 0L, "202600001", LocalDateTime.now()
        )).isInstanceOf(MidReportSubmittedException.class);
    }

    @Test
    @DisplayName("구조화 필드는 올바른 JSON 배열만 저장한다")
    void rejectsMalformedStructuredField() throws Exception {
        MidReport report = report(0L, MidReportStatus.DRAFT);
        JsonNode fields = objectMapper.readTree("""
            [{"key":"guiScreens","value":"not-json"}]
            """);

        assertThatThrownBy(() -> report.updateBlock(
            "gui-design", 0L, fields, "202600001", LocalDateTime.now()
        )).isInstanceOf(InvalidMidReportFieldsException.class);
    }

    @Test
    @DisplayName("GUI 영역 완료는 id, name, description이 채워진 행을 요구한다")
    void validatesGuiRowsOnCompletion() throws Exception {
        MidReport report = report(0L, MidReportStatus.DRAFT);
        JsonNode incomplete = objectMapper.readTree("""
            [{"key":"guiScreens","value":"[{\\"id\\":\\"home\\",\\"name\\":\\"메인\\",\\"description\\":\\"\\"}]"}]
            """);
        report.updateBlock("gui-design", 0L, incomplete, "202600001", LocalDateTime.now());

        assertThatThrownBy(() -> report.completeBlock(
            "gui-design", 0L, "202600001", LocalDateTime.now()
        )).isInstanceOf(MidReportBlockIncompleteException.class);
    }

    private MidReport report(Long version, MidReportStatus status) {
        MidReport created = MidReport.create(
            1L, 2L, "CineFlow 중간보고서", LocalDateTime.of(2026, 10, 26, 23, 59), "CineFlow", "영화관 관리"
        );
        return MidReport.builder()
            .id(10L)
            .teamId(created.getTeamId())
            .milestoneId(created.getMilestoneId())
            .title(created.getTitle())
            .version(version)
            .dueDate(created.getDueDate())
            .status(status)
            .blocks(created.getBlocks())
            .build();
    }

    private JsonNode topicFields() {
        try {
            return objectMapper.readTree("""
                [
                  {"key":"title","label":"무시되는 라벨","value":"CineFlow 2"},
                  {"key":"description","value":"수정 설명","multiline":false}
                ]
                """);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
