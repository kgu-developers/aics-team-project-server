package kgu.developers.admin.importcommon;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipInputStream;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.RecordFormatException;
import org.springframework.web.multipart.MultipartFile;

import kgu.developers.domain.importBatch.exception.ImportBatchFileInvalidException;

public final class Sheets {
    private static final int HEADER_SEARCH_LIMIT = 10;
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final long MAX_TOTAL_UNCOMPRESSED_SIZE = 100 * 1024 * 1024; // 100MB
    private static final int MAX_ROWS = 1000;
    private static final int SPARSE_ROW_MARGIN = 10; // 서식만 있는 빈 행을 감안한 여유 배수
    private static final int BUFFER_SIZE = 8192;
    private static final byte[] ZIP_MAGIC = {'P', 'K', 0x03, 0x04};

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

        Path temp = null;
        try {
            temp = copyToTempFile(file);
            checkUncompressedSize(temp);

            try (Workbook workbook = WorkbookFactory.create(temp.toFile(), null, true)) {
                Sheet sheet = workbook.getSheetAt(0);
                Row header = headerRow(sheet, requiredHeader);
                RowMapper<T> mapper = binder.apply(header);

                // getLastRowNum()은 시트에서 가장 멀리 떨어진 셀의 행 번호라, 맨 아래 근처
                // 한 칸에만 서식이 남아있는 파일이면 실제 데이터가 몇 줄 안 돼도 이 값이
                // 수십만까지 뛸 수 있다. MAX_ROWS는 "채워진 행"만 세므로 그 경우를 못 막아서,
                // 훑을 물리적 행 범위 자체를 먼저 제한한다.
                if (sheet.getLastRowNum() - header.getRowNum() > MAX_ROWS * SPARSE_ROW_MARGIN) {
                    throw new ImportBatchFileInvalidException("행 범위가 너무 넓습니다.");
                }

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
            }
        } catch (ImportBatchFileInvalidException e) {
            throw e;
        } catch (IOException e) {
            throw new ImportBatchFileInvalidException(e);
        } catch (EncryptedDocumentException | IllegalArgumentException | RecordFormatException e) {
            // 비밀번호 보호, 지원하지 않는 파일 형식(NotOfficeXmlFileException 등은
            // IllegalArgumentException 계열), 손상된 레코드 구조처럼 업로드한 파일 자체의
            // 문제로 판단되는 예외만 좁혀서 400으로 응답한다. 그 외(NPE 등 POI/서버 쪽에서
            // 예상 못한 문제)는 여기서 안 잡고 GlobalExceptionHandler의 범용 핸들러가 500으로
            // 처리하게 둔다 — 서버 오류까지 "파일이 잘못됐다"고 클라이언트 탓으로 돌리면 안 된다
            // (sunzx0428 리뷰 09-03).
            throw new ImportBatchFileInvalidException("파일을 열 수 없습니다: " + e.getMessage());
        } finally {
            deleteQuietly(temp);
        }
    }

    private static Path copyToTempFile(MultipartFile file) throws IOException {
        Path temp = Files.createTempFile("import-", ".tmp");
        try (InputStream in = file.getInputStream(); OutputStream out = Files.newOutputStream(temp)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_FILE_SIZE_BYTES) {
                    throw new ImportBatchFileInvalidException(
                        "파일 크기가 " + MAX_FILE_SIZE_BYTES / 1024 / 1024 + "MB를 초과했습니다.");
                }
                out.write(buffer, 0, read);
            }
        } catch (IOException | RuntimeException e) {
            deleteQuietly(temp);
            throw e;
        }
        return temp;
    }

    private static void checkUncompressedSize(Path path) throws IOException {
        if (!isZip(path)) {
            return;
        }

        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            while (zip.getNextEntry() != null) {
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_TOTAL_UNCOMPRESSED_SIZE) {
                        throw new ImportBatchFileInvalidException("압축 해제 총 크기가 "
                            + MAX_TOTAL_UNCOMPRESSED_SIZE / 1024 / 1024 + "MB를 초과했습니다.");
                    }
                }
            }
        }
    }

    private static boolean isZip(Path path) throws IOException {
        byte[] magic = new byte[ZIP_MAGIC.length];
        try (InputStream in = Files.newInputStream(path)) {
            return in.readNBytes(magic, 0, magic.length) == magic.length && Arrays.equals(magic, ZIP_MAGIC);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일 삭제 실패는 요청 처리에 영향을 주지 않는다
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
