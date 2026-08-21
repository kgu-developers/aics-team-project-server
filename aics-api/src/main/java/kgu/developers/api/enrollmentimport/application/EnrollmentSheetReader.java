package kgu.developers.api.enrollmentimport.application;

import static kgu.developers.api.importcommon.RowStatus.INVALID;
import static kgu.developers.api.importcommon.RowStatus.VALID;
import static kgu.developers.api.importcommon.Sheets.cell;
import static kgu.developers.api.importcommon.Sheets.column;
import static kgu.developers.api.importcommon.Sheets.headerRow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.importBatch.exception.ImportBatchFileInvalidException;

public final class EnrollmentSheetReader {
    private static final String SCHOOL_MAIL_DOMAIN = "@kyonggi.ac.kr";

    private EnrollmentSheetReader() {
    }

    public static List<EnrollmentImportRow> read(MultipartFile file) {
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = headerRow(sheet, "학번");
            Columns columns = new Columns(
                column(header, "학번"),
                column(header, "성명", "이름"),
                column(header, "이메일", "메일", "E-mail"),
                column(header, "연락처", "전화번호", "휴대전화"),
                column(header, "역할"));

            List<EnrollmentImportRow> rows = new ArrayList<>();
            for (int i = header.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                EnrollmentImportRow row = toRow(sheet.getRow(i), i + 1, columns);
                if (row != null) {
                    rows.add(row);
                }
            }
            return rows;
        } catch (ImportBatchFileInvalidException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ImportBatchFileInvalidException(e);
        }
    }

    private record Columns(int studentNumber, int name, int email, int phone, int role) {
    }

    private static EnrollmentImportRow toRow(Row row, int rowNumber, Columns columns) {
        if (row == null) {
            return null;
        }
        String studentNumber = cell(row, columns.studentNumber());
        String name = cell(row, columns.name());
        String email = cell(row, columns.email());
        String phone = cell(row, columns.phone());
        String roleText = cell(row, columns.role());

        if (studentNumber.isEmpty() && name.isEmpty()) {
            return null;
        }
        if (studentNumber.isEmpty()) {
            return invalid(rowNumber, studentNumber, name, email, phone, "학번이 비어 있습니다.");
        }
        if (name.isEmpty()) {
            return invalid(rowNumber, studentNumber, name, email, phone, "이름이 비어 있습니다.");
        }

        Role role = parseRole(roleText);
        if (role == null) {
            return invalid(rowNumber, studentNumber, name, email, phone, "역할은 학생 또는 조교만 가능합니다.");
        }
        if (email.isEmpty()) {
            email = studentNumber + SCHOOL_MAIL_DOMAIN;
        }
        return new EnrollmentImportRow(rowNumber, studentNumber, name, email, phone, role, VALID, null);
    }

    private static EnrollmentImportRow invalid(int rowNumber, String studentNumber, String name,
        String email, String phone, String message) {
        return new EnrollmentImportRow(rowNumber, studentNumber, name, email, phone, null, INVALID, message);
    }

    private static Role parseRole(String roleText) {
        return switch (roleText) {
            case "", "학생", "STUDENT", "student" -> Role.STUDENT;
            case "조교", "ASSISTANT", "assistant" -> Role.ASSISTANT;
            default -> null;
        };
    }
}
