package kgu.developers.admin.teamimport.application;

import static kgu.developers.admin.importcommon.RowStatus.INVALID;
import static kgu.developers.admin.importcommon.RowStatus.VALID;
import static kgu.developers.admin.importcommon.Sheets.cell;
import static kgu.developers.admin.importcommon.Sheets.column;
import static kgu.developers.admin.importcommon.Sheets.tooLong;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Row;
import org.springframework.web.multipart.MultipartFile;

import kgu.developers.admin.importcommon.Sheets;

public final class TeamSheetReader {
    private static final List<String> LEADER_MARKS =
        List.of("Y", "y", "O", "o", "TRUE", "true", "1", "팀장", "리더");
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(01[016789]-?\\d{3,4}-?\\d{4}|02-?\\d{3,4}-?\\d{4}|0[3-9][0-9]-?\\d{3,4}-?\\d{4}|01[016789]\\d{7,8}|02\\d{7,8}|0[3-9][0-9]\\d{7,8})$"
    );

    private static final Pattern GRADE_PATTERN = Pattern.compile("^[1-4](학년)?$");

    private TeamSheetReader() {
    }

    public static List<TeamImportRow> read(MultipartFile file) {
        return Sheets.read(file, "학번", header -> {
            Columns columns = new Columns(
                column(header, "팀명", "팀", "조", "조명"),
                column(header, "학번"),
                column(header, "성명", "이름"),
                column(header, "팀장", "팀장여부", "리더"),
                column(header, "역할", "담당", "담당역할"),
                column(header, "전화번호", "연락처", "폰", "전화"),
                column(header, "학년"));
            return (row, rowNumber) -> toRow(row, rowNumber, columns);
        });
    }

    private record Columns(int teamName, int studentNumber, int name, int leader, int projectRole, int phoneNumber, int grade) {
    }

    private static TeamImportRow toRow(Row row, int rowNumber, Columns columns) {
        if (row == null) {
            return null;
        }
        String teamName = cell(row, columns.teamName());
        String studentNumber = cell(row, columns.studentNumber());
        String name = cell(row, columns.name());
        boolean leader = LEADER_MARKS.contains(cell(row, columns.leader()));
        String projectRole = cell(row, columns.projectRole());
        String phoneNumber = cell(row, columns.phoneNumber());
        String grade = cell(row, columns.grade());

        if (teamName.isEmpty() && studentNumber.isEmpty() && name.isEmpty()) {
            return null;
        }
        if (teamName.isEmpty()) {
            return invalid(rowNumber, teamName, studentNumber, name, leader, projectRole, phoneNumber, grade, "팀명이 비어 있습니다.");
        }
        if (studentNumber.isEmpty()) {
            return invalid(rowNumber, teamName, studentNumber, name, leader, projectRole, phoneNumber, grade, "학번이 비어 있습니다.");
        }

        // 저장 시 잘리거나 실패하지 않도록 team/team_member 컬럼 길이를 미리 확인한다
        String tooLong = tooLong("팀명", teamName, 200);
        tooLong = tooLong != null ? tooLong : tooLong("학번", studentNumber, 16);
        tooLong = tooLong != null ? tooLong : tooLong("역할", projectRole, 50);
        tooLong = tooLong != null ? tooLong : tooLong("전화번호", phoneNumber, 20);
        tooLong = tooLong != null ? tooLong : tooLong("학년", grade, 10);
        if (tooLong != null) {
            return invalid(rowNumber, teamName, studentNumber, name, leader, projectRole, phoneNumber, grade, tooLong);
        }
        
        // 전화번호 형식 검증 (빈 값은 허용)
        if (!phoneNumber.isEmpty() && !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            return invalid(rowNumber, teamName, studentNumber, name, leader, projectRole, phoneNumber, grade, 
                "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)");
        }

        // 학년 형식 검증 (빈 값은 허용)
        if (!grade.isEmpty() && !GRADE_PATTERN.matcher(grade).matches()) {
            return invalid(rowNumber, teamName, studentNumber, name, leader, projectRole, phoneNumber, grade,
                "학년 형식이 올바르지 않습니다. (예: 1 또는 1학년)");
        }

        return new TeamImportRow(rowNumber, teamName, studentNumber, name, leader, projectRole,
            format(phoneNumber), grade, VALID, null);
    }

    // 시트마다 제각각인 표기(01012345678, 010-12345678 ...)를 010-1234-5678 한 가지로 맞춘다.
    // 형식 검증을 통과한 값만 들어오므로 자릿수는 믿고 잘라도 된다
    private static String format(String phoneNumber) {
        String digits = phoneNumber.replace("-", "");
        if (digits.isEmpty()) {
            return phoneNumber;
        }
        int head = digits.startsWith("02") ? 2 : 3;
        int tail = digits.length() - 4;
        return digits.substring(0, head) + "-" + digits.substring(head, tail) + "-" + digits.substring(tail);
    }

    private static TeamImportRow invalid(int rowNumber, String teamName, String studentNumber, String name,
        boolean leader, String projectRole, String phoneNumber, String grade, String message) {
        return new TeamImportRow(rowNumber, teamName, studentNumber, name, leader, projectRole, phoneNumber, grade, INVALID, message);
    }
}
