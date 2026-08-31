package kgu.developers.admin.importcommon;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import kgu.developers.domain.importBatch.exception.ImportBatchFileInvalidException;

public final class Sheets {
    private static final int HEADER_SEARCH_LIMIT = 10;
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final long MAX_TOTAL_UNCOMPRESSED_SIZE = 100 * 1024 * 1024; // 100MB
    private static final int MAX_ROWS = 1000;

    private Sheets() {
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(Row row, int rowNumber);
    }

    public static <T> List<T> read(MultipartFile file, String requiredHeader,
        Function<Row, RowMapper<T>> binder) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ImportBatchFileInvalidException(
                "파일 크기가 " + MAX_FILE_SIZE_BYTES / 1024 / 1024 + "MB를 초과했습니다.");
        }

        ZipSecureFile.setMinInflateRatio(0.01);
        ZipSecureFile.setMaxEntrySize(MAX_FILE_SIZE_BYTES);

        try (InputStream in = file.getInputStream(); 
             CountingInputStream countingIn = new CountingInputStream(in, MAX_TOTAL_UNCOMPRESSED_SIZE);
             Workbook workbook = WorkbookFactory.create(countingIn)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = headerRow(sheet, requiredHeader);
            RowMapper<T> mapper = binder.apply(header);

            List<T> rows = new ArrayList<>();
            for (int i = header.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                T row = mapper.map(sheet.getRow(i), i + 1);
                if (row != null) {
                    rows.add(row);
                    if (rows.size() > MAX_ROWS) {
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

    private static class CountingInputStream extends FilterInputStream {
        private final long maxSize;
        private long bytesRead = 0;

        CountingInputStream(InputStream in, long maxSize) {
            super(in);
            this.maxSize = maxSize;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                bytesRead++;
                checkLimit();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int result = super.read(b, off, len);
            if (result > 0) {
                bytesRead += result;
                checkLimit();
            }
            return result;
        }

        private void checkLimit() throws IOException {
            if (bytesRead > maxSize) {
                throw new ImportBatchFileInvalidException(
                    "압축 해제 총 크기가 " + maxSize / 1024 / 1024 + "MB를 초과했습니다.");
            }
        }
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
        return index < 0 ? "" : new DataFormatter().formatCellValue(row.getCell(index)).trim();
    }

    public static String tooLong(String label, String value, int max) {
        return value.length() > max ? label + " 값이 " + max + "자를 넘습니다." : null;
    }
}
