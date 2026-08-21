package kgu.developers.api.importcommon;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import kgu.developers.domain.importBatch.exception.ImportBatchFileInvalidException;

/** 명단 엑셀은 양식이 조금씩 다르므로, 컬럼 위치 대신 헤더 이름으로 찾는다. */
public final class Sheets {
    private static final DataFormatter FORMATTER = new DataFormatter();
    private static final int HEADER_SEARCH_LIMIT = 10;

    private Sheets() {
    }

    /** 위에서부터 훑어 지정한 헤더가 있는 행을 찾는다. 없으면 읽을 수 없는 파일로 본다. */
    public static Row headerRow(Sheet sheet, String requiredHeader) {
        int last = Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + HEADER_SEARCH_LIMIT);
        for (int i = sheet.getFirstRowNum(); i <= last; i++) {
            if (column(sheet.getRow(i), requiredHeader) >= 0) {
                return sheet.getRow(i);
            }
        }
        throw new ImportBatchFileInvalidException(requiredHeader + " 컬럼을 찾을 수 없습니다.");
    }

    /** 헤더 이름이 일치하는 컬럼 번호. 못 찾으면 -1이며, 그 컬럼은 빈 값으로 읽힌다. */
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
}
