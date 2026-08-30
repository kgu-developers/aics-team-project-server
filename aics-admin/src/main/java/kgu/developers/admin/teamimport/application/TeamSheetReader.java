package kgu.developers.admin.teamimport.application;

import static kgu.developers.admin.importcommon.RowStatus.INVALID;
import static kgu.developers.admin.importcommon.RowStatus.VALID;
import static kgu.developers.admin.importcommon.Sheets.cell;
import static kgu.developers.admin.importcommon.Sheets.column;
import static kgu.developers.admin.importcommon.Sheets.tooLong;

import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.springframework.web.multipart.MultipartFile;

import kgu.developers.admin.importcommon.Sheets;

public final class TeamSheetReader {
    private static final List<String> LEADER_MARKS =
        List.of("Y", "y", "O", "o", "TRUE", "true", "1", "팀장", "리더");

    private TeamSheetReader() {
    }

    public static List<TeamImportRow> read(MultipartFile file) {
        return Sheets.read(file, "학번", header -> {
            Columns columns = new Columns(
                column(header, "팀명", "팀", "조", "조명"),
                column(header, "학번"),
                column(header, "성명", "이름"),
                column(header, "팀장", "팀장여부", "리더"),
                column(header, "역할", "담당", "담당역할"));
            return (row, rowNumber) -> toRow(row, rowNumber, columns);
        });
    }

    private record Columns(int teamName, int studentNumber, int name, int leader, int projectRole) {
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

        if (teamName.isEmpty() && studentNumber.isEmpty() && name.isEmpty()) {
            return null;
        }
        if (teamName.isEmpty()) {
            return invalid(rowNumber, teamName, studentNumber, name, leader, projectRole, "팀명이 비어 있습니다.");
        }
        if (studentNumber.isEmpty()) {
            return invalid(rowNumber, teamName, studentNumber, name, leader, projectRole, "학번이 비어 있습니다.");
        }

        // 저장 시 잘리거나 실패하지 않도록 team/team_member 컬럼 길이를 미리 확인한다
        String tooLong = tooLong("팀명", teamName, 200);
        tooLong = tooLong != null ? tooLong : tooLong("학번", studentNumber, 16);
        tooLong = tooLong != null ? tooLong : tooLong("역할", projectRole, 50);
        if (tooLong != null) {
            return invalid(rowNumber, teamName, studentNumber, name, leader, projectRole, tooLong);
        }
        return new TeamImportRow(rowNumber, teamName, studentNumber, name, leader, projectRole, VALID, null);
    }

    private static TeamImportRow invalid(int rowNumber, String teamName, String studentNumber, String name,
        boolean leader, String projectRole, String message) {
        return new TeamImportRow(rowNumber, teamName, studentNumber, name, leader, projectRole, INVALID, message);
    }
}
