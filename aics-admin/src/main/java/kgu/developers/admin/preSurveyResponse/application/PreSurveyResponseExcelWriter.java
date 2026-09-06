package kgu.developers.admin.preSurveyResponse.application;

import static java.util.stream.Collectors.joining;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.StreamSupport;

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
                row.createCell(1).setCellValue(source.name());  // 탈퇴로 사라진 계정이면 빈 칸

                PreSurveyResponse response = source.response();
                if (response == null) {
                    row.createCell(5).setCellValue(NOT_SUBMITTED);
                    continue;
                }
                row.createCell(2).setCellValue(preferredRoles(response.getPreferredRoles()));
                row.createCell(3).setCellValue(response.getTopicOpinion());
                row.createCell(4).setCellValue(response.getEtcOpinion());
                row.createCell(5).setCellValue(response.getSubmittedAt().format(SUBMITTED_AT_FORMATTER));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // 희망 역할은 형식 제약이 없는 jsonb라 배열이면 사람이 읽을 수 있게 풀고, 그 외 형태는 원본 그대로 적는다.
    private static String preferredRoles(JsonNode preferredRoles) {
        if (!preferredRoles.isArray()) {
            return preferredRoles.toString();
        }
        return StreamSupport.stream(preferredRoles.spliterator(), false)
                .map(JsonNode::asText)
                .collect(joining(", "));
    }
}
