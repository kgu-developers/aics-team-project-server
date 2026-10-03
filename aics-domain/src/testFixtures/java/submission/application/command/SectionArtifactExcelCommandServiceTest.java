package submission.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.submission.application.command.SectionArtifactExcelCommandService;
import kgu.developers.domain.submission.application.query.SectionArtifactMember;
import kgu.developers.domain.submission.application.query.SectionArtifactStageRow;
import kgu.developers.domain.submission.application.query.SectionArtifactTeamRow;
import kgu.developers.domain.submission.domain.SubmissionStatus;

class SectionArtifactExcelCommandServiceTest {

    private static final String[] SUMMARY_HEADERS = {
            "분반", "팀", "팀원", "회의록 수", "회의록 수정 로그 수", "제출 이력 단계 수", "마감된 미제출 단계 수"};
    private static final String[] STAGE_HEADERS = {
            "분반", "팀", "팀원", "단계", "마감 시각", "상태", "첫 제출", "최신 제출", "최신 버전",
            "최초 제출 지각 여부", "최신 파일 수", "이미지 수", "전체 파일 용량"};

    private final SectionArtifactExcelCommandService commandService = new SectionArtifactExcelCommandService();

    @Test
    @DisplayName("팀별 요약·단계별 제출 현황 두 시트를 정해진 열 순서로 만든다")
    void writeWorkbook_WritesTwoSheetsWithHeaders() throws Exception {
        try (Workbook workbook = open(commandService.writeWorkbook("OOP-01", List.of(teamRow(submittedStage()))))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(headerOf(workbook.getSheet("팀별 요약"))).containsExactly(SUMMARY_HEADERS);
            assertThat(headerOf(workbook.getSheet("단계별 제출 현황"))).containsExactly(STAGE_HEADERS);
        }
    }

    @Test
    @DisplayName("제출된 단계는 상태·제출 시각·버전·파일 집계를 그대로 적고 용량에 천 단위 구분을 준다")
    void writeWorkbook_WritesSubmittedStage() throws Exception {
        try (Workbook workbook = open(commandService.writeWorkbook("OOP-01", List.of(teamRow(submittedStage()))))) {
            Row summary = workbook.getSheet("팀별 요약").getRow(1);
            assertThat(summary.getCell(0).getStringCellValue()).isEqualTo("OOP-01");
            assertThat(summary.getCell(1).getStringCellValue()).isEqualTo("1팀");
            assertThat(summary.getCell(2).getStringCellValue()).isEqualTo("20261234 김철수, 20261235 (탈퇴한 사용자)");
            assertThat(summary.getCell(3).getNumericCellValue()).isEqualTo(8);
            assertThat(summary.getCell(4).getNumericCellValue()).isEqualTo(3);
            assertThat(summary.getCell(5).getNumericCellValue()).isEqualTo(1);
            assertThat(summary.getCell(6).getNumericCellValue()).isZero();

            Row stage = workbook.getSheet("단계별 제출 현황").getRow(1);
            assertThat(stage.getCell(3).getStringCellValue()).isEqualTo("중간점검");
            assertThat(stage.getCell(4).getStringCellValue()).isEqualTo("2026-11-15 23:59");
            assertThat(stage.getCell(5).getStringCellValue()).isEqualTo("SUBMITTED");
            assertThat(stage.getCell(6).getStringCellValue()).isEqualTo("2026-11-14 18:20");
            assertThat(stage.getCell(7).getStringCellValue()).isEqualTo("2026-11-16 09:10");
            assertThat(stage.getCell(8).getNumericCellValue()).isEqualTo(3);
            assertThat(stage.getCell(9).getStringCellValue()).isEqualTo("아니오");
            assertThat(stage.getCell(10).getNumericCellValue()).isEqualTo(2);
            assertThat(stage.getCell(11).getNumericCellValue()).isEqualTo(1);
            assertThat(stage.getCell(12).getNumericCellValue()).isEqualTo(4_194_304);
            // 용량은 byte 숫자가 커서 천 단위 구분 표시가 있어야 읽을 수 있다
            assertThat(stage.getCell(12).getCellStyle().getDataFormatString()).isEqualTo("#,##0");
        }
    }

    @Test
    @DisplayName("미제출 단계는 제출 시각·버전·지각 여부를 빈 셀로 남긴다")
    void writeWorkbook_LeavesUnknownCellsEmpty() throws Exception {
        SectionArtifactStageRow notSubmitted = new SectionArtifactStageRow(
                MilestoneType.FINAL_REPORT, LocalDateTime.of(2026, 11, 20, 23, 59),
                SubmissionStatus.NOT_SUBMITTED, null, null, null, null, true, 0, 0, 0L);

        try (Workbook workbook = open(commandService.writeWorkbook("OOP-01", List.of(teamRow(notSubmitted))))) {
            Row stage = workbook.getSheet("단계별 제출 현황").getRow(1);
            assertThat(stage.getCell(5).getStringCellValue()).isEqualTo("NOT_SUBMITTED");
            assertThat(stage.getCell(6).getStringCellValue()).isEmpty();
            assertThat(stage.getCell(7).getStringCellValue()).isEmpty();
            // 셀 자체가 없으면 읽는 쪽에서 null이 되므로 빈 값이라도 셀은 있어야 한다
            assertThat(stage.getCell(8)).isNotNull();
            assertThat(stage.getCell(8).getStringCellValue()).isEmpty();
            assertThat(stage.getCell(9).getStringCellValue()).isEmpty();
        }
    }

    @Test
    @DisplayName("단계 이름은 마일스톤 유형을 사람이 읽는 이름으로 적는다")
    void writeWorkbook_WritesStageLabels() throws Exception {
        List<SectionArtifactStageRow> stages = List.of(
                stage(MilestoneType.PROPOSAL), stage(MilestoneType.MID_REPORT),
                stage(MilestoneType.PRESENTATION), stage(MilestoneType.FINAL_REPORT),
                stage(MilestoneType.GENERAL));

        try (Workbook workbook = open(commandService.writeWorkbook("OOP-01", List.of(teamRow(stages))))) {
            Sheet sheet = workbook.getSheet("단계별 제출 현황");
            assertThat(List.of(
                    sheet.getRow(1).getCell(3).getStringCellValue(),
                    sheet.getRow(2).getCell(3).getStringCellValue(),
                    sheet.getRow(3).getCell(3).getStringCellValue(),
                    sheet.getRow(4).getCell(3).getStringCellValue(),
                    sheet.getRow(5).getCell(3).getStringCellValue()))
                    .containsExactly("제안서", "중간점검", "발표 자료", "최종 보고서", "GENERAL");
        }
    }

    @Test
    @DisplayName("팀명이 수식으로 읽힐 수 있는 문자로 시작하면 텍스트 셀로 고정한다")
    void writeWorkbook_QuotesFormulaLikeTeamName() throws Exception {
        SectionArtifactTeamRow row = new SectionArtifactTeamRow(
                20L, "=HYPERLINK(\"http://evil\")", List.of(new SectionArtifactMember("20261234", "김철수")),
                0, 0, List.of(stage(MilestoneType.PROPOSAL)));

        try (Workbook workbook = open(commandService.writeWorkbook("OOP-01", List.of(row)))) {
            assertThat(workbook.getSheet("팀별 요약").getRow(1).getCell(1).getCellStyle().getQuotePrefixed())
                    .isTrue();
            // 평범한 값에는 인용 접두를 붙이지 않는다
            assertThat(workbook.getSheet("팀별 요약").getRow(1).getCell(0).getCellStyle().getQuotePrefixed())
                    .isFalse();
        }
    }

    @Test
    @DisplayName("팀이 없으면 머리글만 있는 통합문서를 만든다")
    void writeWorkbook_WritesHeaderOnlyWhenNoTeam() throws Exception {
        try (Workbook workbook = open(commandService.writeWorkbook("OOP-01", List.of()))) {
            assertThat(workbook.getSheet("팀별 요약").getLastRowNum()).isZero();
            assertThat(workbook.getSheet("단계별 제출 현황").getLastRowNum()).isZero();
        }
    }

    private Workbook open(byte[] content) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(content));
    }

    private List<String> headerOf(Sheet sheet) {
        Row header = sheet.getRow(0);
        return java.util.stream.IntStream.range(0, header.getLastCellNum())
                .mapToObj(column -> header.getCell(column).getStringCellValue())
                .toList();
    }

    private SectionArtifactTeamRow teamRow(SectionArtifactStageRow stage) {
        return teamRow(List.of(stage));
    }

    private SectionArtifactTeamRow teamRow(List<SectionArtifactStageRow> stages) {
        return new SectionArtifactTeamRow(
                20L,
                "1팀",
                // 이름이 없는 팀원은 대체 문구로 적힌다
                List.of(new SectionArtifactMember("20261234", "김철수"), new SectionArtifactMember("20261235", null)),
                8,
                3,
                stages);
    }

    private SectionArtifactStageRow submittedStage() {
        return new SectionArtifactStageRow(
                MilestoneType.MID_REPORT,
                LocalDateTime.of(2026, 11, 15, 23, 59),
                SubmissionStatus.SUBMITTED,
                LocalDateTime.of(2026, 11, 14, 18, 20),
                LocalDateTime.of(2026, 11, 16, 9, 10),
                3,
                false,
                false,
                2,
                1,
                4_194_304L);
    }

    private SectionArtifactStageRow stage(MilestoneType type) {
        return new SectionArtifactStageRow(type, LocalDateTime.of(2026, 11, 15, 23, 59),
                SubmissionStatus.NOT_SUBMITTED, null, null, null, null, false, 0, 0, 0L);
    }
}
