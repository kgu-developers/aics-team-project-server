package kgu.developers.domain.submission.application.command;

import static java.util.stream.Collectors.joining;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.submission.application.query.SectionArtifactMember;
import kgu.developers.domain.submission.application.query.SectionArtifactStageRow;
import kgu.developers.domain.submission.application.query.SectionArtifactTeamRow;

@Service
public class SectionArtifactExcelCommandService {
    private static final String[] SUMMARY_HEADERS = {
            "분반", "팀", "팀원", "회의록 수", "회의록 수정 로그 수", "제출 이력 단계 수", "마감된 미제출 단계 수"};
    private static final String[] STAGE_HEADERS = {
            "분반", "팀", "팀원", "단계", "마감 시각", "상태", "첫 제출", "최신 제출", "최신 버전",
            "최초 제출 지각 여부", "최신 파일 수", "이미지 수", "전체 파일 용량"};
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    // 팀명·이름은 사용자가 입력한 값이라 =HYPERLINK(...)처럼 수식으로 읽힐 수 있는 문자로 시작할 수 있다.
    private static final String FORMULA_STARTERS = "=+-@";
    // 용량은 byte 단위 숫자라 자릿수가 커서, 천 단위 구분 없이는 눈으로 읽기 어렵다.
    private static final String SIZE_FORMAT = "#,##0";

    public byte[] writeWorkbook(String sectionLabel, List<SectionArtifactTeamRow> teamRows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setQuotePrefixed(true);
            CellStyle sizeStyle = workbook.createCellStyle();
            sizeStyle.setDataFormat(workbook.createDataFormat().getFormat(SIZE_FORMAT));

            writeSummarySheet(workbook.createSheet("팀별 요약"), sectionLabel, teamRows, textStyle);
            writeStageSheet(workbook.createSheet("단계별 제출 현황"), sectionLabel, teamRows, textStyle, sizeStyle);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeSummarySheet(
            Sheet sheet,
            String sectionLabel,
            List<SectionArtifactTeamRow> teamRows,
            CellStyle textStyle
    ) {
        writeHeader(sheet, SUMMARY_HEADERS);
        int rowNumber = 1;
        for (SectionArtifactTeamRow source : teamRows) {
            Row row = sheet.createRow(rowNumber++);
            writeText(row, 0, sectionLabel, textStyle);
            writeText(row, 1, source.teamName(), textStyle);
            writeText(row, 2, members(source.members()), textStyle);
            row.createCell(3).setCellValue(source.meetingRecordCount());
            row.createCell(4).setCellValue(source.meetingRecordEditCount());
            row.createCell(5).setCellValue(source.submittedStageCount());
            row.createCell(6).setCellValue(source.overdueMissingStageCount());
        }
    }

    private void writeStageSheet(
            Sheet sheet,
            String sectionLabel,
            List<SectionArtifactTeamRow> teamRows,
            CellStyle textStyle,
            CellStyle sizeStyle
    ) {
        writeHeader(sheet, STAGE_HEADERS);
        int rowNumber = 1;
        for (SectionArtifactTeamRow teamRow : teamRows) {
            String members = members(teamRow.members());
            for (SectionArtifactStageRow source : teamRow.stages()) {
                Row row = sheet.createRow(rowNumber++);
                writeText(row, 0, sectionLabel, textStyle);
                writeText(row, 1, teamRow.teamName(), textStyle);
                writeText(row, 2, members, textStyle);
                writeText(row, 3, stageLabel(source.type()), textStyle);
                writeText(row, 4, format(source.dueAt()), textStyle);
                writeText(row, 5, source.status().name(), textStyle);
                writeText(row, 6, format(source.firstSubmittedAt()), textStyle);
                writeText(row, 7, format(source.lastSubmittedAt()), textStyle);
                // 제출 이력이 없으면 빈 값이다. 셀 자체를 비워두면 읽는 쪽에서 null이 되므로 항상 만든다.
                if (source.latestVersion() == null) {
                    writeText(row, 8, "", textStyle);
                } else {
                    row.createCell(8).setCellValue(source.latestVersion());
                }
                writeText(row, 9, lateMark(source.firstSubmissionLate()), textStyle);
                row.createCell(10).setCellValue(source.fileCount());
                row.createCell(11).setCellValue(source.imageCount());
                Cell totalFileSize = row.createCell(12);
                totalFileSize.setCellValue(source.totalFileSize());
                totalFileSize.setCellStyle(sizeStyle);
            }
        }
    }

    private void writeHeader(Sheet sheet, String[] headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
    }

    private String members(List<SectionArtifactMember> members) {
        return members.stream()
                .map(member -> member.studentNumber() + " " + member.displayName())
                .collect(joining(", "));
    }

    private String stageLabel(MilestoneType type) {
        return switch (type) {
            case PROPOSAL -> "제안서";
            case MID_REPORT -> "중간점검";
            case PRESENTATION -> "발표 자료";
            case FINAL_REPORT -> "최종 보고서";
            default -> type.name();
        };
    }

    // 마감 시각이 없거나 제출 이력이 없어 지각 여부를 판단할 수 없는 단계는 빈 값으로 둔다.
    private String lateMark(Boolean late) {
        if (late == null) {
            return "";
        }
        return late ? "예" : "아니오";
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMATTER);
    }

    private void writeText(Row row, int column, String value, CellStyle textStyle) {
        Cell cell = row.createCell(column);
        String text = value == null ? "" : value;
        cell.setCellValue(text);
        if (!text.isEmpty() && FORMULA_STARTERS.indexOf(text.charAt(0)) >= 0) {
            cell.setCellStyle(textStyle);
        }
    }
}
