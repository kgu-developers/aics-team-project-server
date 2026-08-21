package kgu.developers.admin.importcommon;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import kgu.developers.domain.importBatch.exception.ImportBatchFileInvalidException;

public final class Sheets {
    private static final DataFormatter FORMATTER = new DataFormatter();
    private static final int HEADER_SEARCH_LIMIT = 10;

    private Sheets() {
    }

    public static Row headerRow(Sheet sheet, String requiredHeader) {
        int last = Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + HEADER_SEARCH_LIMIT);
        for (int i = sheet.getFirstRowNum(); i <= last; i++) {
            if (column(sheet.getRow(i), requiredHeader) >= 0) {
                return sheet.getRow(i);
            }
        }
        throw new ImportBatchFileInvalidException(requiredHeader + " 컬럼을 찾을 수 없습니다.");
    }

    public static int column(Row header, String... names) {
        if (header == null) {
            return -1;
        }
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String value = cell(header, c);
            for (String name : names) {
                if (value.equals(name)) {
                    return c;
                }
            }
        }
        return -1;
    }

    public static String cell(Row row, int index) {
        return index < 0 ? "" : FORMATTER.formatCellValue(row.getCell(index)).trim();
    }

    public static String tooLong(String label, String value, int max) {
        return value.length() > max ? label + " 값이 " + max + "자를 넘습니다." : null;
    }
}
