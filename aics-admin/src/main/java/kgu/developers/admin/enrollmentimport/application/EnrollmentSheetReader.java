package kgu.developers.admin.enrollmentimport.application;

import static kgu.developers.admin.importcommon.RowStatus.INVALID;
import static kgu.developers.admin.importcommon.RowStatus.VALID;
import static kgu.developers.admin.importcommon.Sheets.cell;
import static kgu.developers.admin.importcommon.Sheets.column;
import static kgu.developers.admin.importcommon.Sheets.headerRow;
import static kgu.developers.admin.importcommon.Sheets.tooLong;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.importBatch.exception.ImportBatchFileInvalidException;

public final class EnrollmentSheetReader {
    private static final String SCHOOL_MAIL_DOMAIN = "@kyonggi.ac.kr";
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MAX_ROWS = 1000;

    private EnrollmentSheetReader() {
    }

    public static List<EnrollmentImportRow> read(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ImportBatchFileInvalidException("파일 크기가 10MB를 초과했습니다.");
        }

        ZipSecureFile.setMinInflateRatio(0.01);
        ZipSecureFile.setMaxEntrySize(MAX_FILE_SIZE_BYTES);

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
            int dataRowCount = 0;
            for (int i = header.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                EnrollmentImportRow row = toRow(sheet.getRow(i), i + 1, columns);
                if (row != null) {
                    rows.add(row);
                    dataRowCount++;
                    if (dataRowCount > MAX_ROWS) {
                        throw new ImportBatchFileInvalidException("행 수가 " + MAX_ROWS + "행을 초과했습니다.");
                    }
                }
            }
            return rows;
        } catch (ImportBatchFileInvalidException e) {
            throw e;
        } catch (IOException e) {
            throw new ImportBatchFileInvalidException(e);
        } catch (Exception e) {
            throw new ImportBatchFileInvalidException("압축 파일 처리 중 오류가 발생했습니다: " + e.getMessage());
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

        // 저장 시 잘리거나 실패하지 않도록 user 테이블 컬럼 길이를 미리 확인한다
        String tooLong = tooLong("학번", studentNumber, 16);
        tooLong = tooLong != null ? tooLong : tooLong("이름", name, 32);
        tooLong = tooLong != null ? tooLong : tooLong("이메일", email, 64);
        tooLong = tooLong != null ? tooLong : tooLong("연락처", phone, 20);
        if (tooLong != null) {
            return invalid(rowNumber, studentNumber, name, email, phone, tooLong);
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
