package kgu.developers.admin.preSurveyResponse.application;

import static java.util.stream.Collectors.joining;
import static kgu.developers.admin.preSurveyResponse.presentation.response.PreSurveyResponseAdminListResponse.WITHDRAWN_USER_NAME;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseRow;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;

// 조회한 행을 xlsx 바이트로 만든다. DB 조회와 분리해 두어야 통합문서를 만드는 동안 트랜잭션과
// 커넥션을 붙잡지 않는다.
public final class PreSurveyResponseExcelWriter {

    private static final String[] HEADERS = {"학번", "이름", "희망 역할", "주제 의견", "기타 의견", "제출일"};
    private static final DateTimeFormatter SUBMITTED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String NOT_SUBMITTED = "미제출";
    // 의견에는 길이 제한이 없어서 이미 쌓인 응답 중에도 셀 한도를 넘는 값이 있을 수 있다. 그대로
    // 넣으면 POI가 IllegalArgumentException을 던져 분반 전체 다운로드가 500이 되므로, 넘치는
    // 값은 잘라 넣고 잘렸다는 사실을 셀에 남긴다. 원문은 목록 조회 API로 확인할 수 있다.
    private static final int MAX_CELL_LENGTH = SpreadsheetVersion.EXCEL2007.getMaxTextLength();
    private static final String TRUNCATED_MARK = "…(이하 생략)";

    private PreSurveyResponseExcelWriter() {
    }

    public static byte[] write(List<PreSurveyResponseRow> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("사전조사 응답");

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }

            int rowNumber = 1;
            for (PreSurveyResponseRow source : rows) {
                Row row = sheet.createRow(rowNumber++);
                row.createCell(0).setCellValue(source.userId());
                row.createCell(1).setCellValue(
                        fit(source.name() == null ? WITHDRAWN_USER_NAME : source.name()));

                PreSurveyResponse response = source.response();
                if (response == null) {
                    row.createCell(5).setCellValue(NOT_SUBMITTED);
                    continue;
                }
                row.createCell(2).setCellValue(fit(preferredRoles(response.getPreferredRoles())));
                row.createCell(3).setCellValue(fit(response.getTopicOpinion()));
                row.createCell(4).setCellValue(fit(response.getEtcOpinion()));
                row.createCell(5).setCellValue(response.getSubmittedAt().format(SUBMITTED_AT_FORMATTER));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // 셀 한도를 넘는 값만 잘라 표시를 붙인다. null은 그대로 둬 빈 셀로 들어가게 한다.
    private static String fit(String value) {
        if (value == null || value.length() <= MAX_CELL_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_CELL_LENGTH - TRUNCATED_MARK.length()) + TRUNCATED_MARK;
    }

    // 희망 역할은 형식 제약이 없는 jsonb라 배열이면 사람이 읽을 수 있게 풀고, 그 외 형태는 원본 그대로 적는다.
    private static String preferredRoles(JsonNode preferredRoles) {
        if (preferredRoles == null) {
            return "";
        }
        if (!preferredRoles.isArray()) {
            return preferredRoles.toString();
        }
        return StreamSupport.stream(preferredRoles.spliterator(), false)
                .map(JsonNode::asText)
                .collect(joining(", "));
    }
}
