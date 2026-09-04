package importcommon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import kgu.developers.admin.importcommon.Sheets;
import kgu.developers.domain.importBatch.exception.ImportBatchFileInvalidException;

public class SheetsTest {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final int PADDING_ENTRIES = 12;
    private static final int PADDING_ENTRY_MEGABYTES = 9;
    private static final double POI_MIN_INFLATE_RATIO = 0.01;

    @Test
    @DisplayName("read는 정상 엑셀 파일의 데이터 행을 읽는다")
    public void read_ReadsRows() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "enrollments.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel());

        List<String> rows = Sheets.read(file, "학번", header -> (row, rowNumber) -> Sheets.cell(row, 0));

        assertThat(rows).containsExactly("202400001", "202400002");
    }

    @Test
    @DisplayName("read는 원본이 10MB 이하여도 압축 해제 총 크기가 100MB를 넘으면 거부한다")
    public void read_RejectsZipBomb() throws IOException {
        byte[] bomb = zipBomb();
        MockMultipartFile file = new MockMultipartFile("file", "enrollments.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bomb);

        assertThat(file.getSize()).isLessThanOrEqualTo(MAX_FILE_SIZE_BYTES);
        assertThat((double)bomb.length / paddingSize()).isGreaterThan(POI_MIN_INFLATE_RATIO);
        assertThatThrownBy(() -> Sheets.read(file, "학번", header -> (row, rowNumber) -> Sheets.cell(row, 0)))
            .isInstanceOf(ImportBatchFileInvalidException.class)
            .hasRootCauseMessage("압축 해제 총 크기가 100MB를 초과했습니다.");
    }

    @Test
    @DisplayName("read는 10MB를 초과하는 파일을 거부한다")
    public void read_RejectsOversizedFile() {
        MockMultipartFile file = new MockMultipartFile("file", "enrollments.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[(int)MAX_FILE_SIZE_BYTES + 1]);

        assertThatThrownBy(() -> Sheets.read(file, "학번", header -> (row, rowNumber) -> Sheets.cell(row, 0)))
            .isInstanceOf(ImportBatchFileInvalidException.class)
            .hasRootCauseMessage("파일 크기가 10MB를 초과했습니다.");
    }

    private byte[] excel() throws IOException {
        String[][] all = {
            {"학번", "성명"},
            {"202400001", "홍길동"},
            {"202400002", "김철수"}
        };
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            for (int i = 0; i < all.length; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < all[i].length; j++) {
                    row.createCell(j).setCellValue(all[i][j]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] zipBomb() throws IOException {
        byte[] chunk = paddingChunk();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream target = new ZipOutputStream(out);
             ZipInputStream source = new ZipInputStream(new ByteArrayInputStream(excel()))) {
            ZipEntry entry;
            while ((entry = source.getNextEntry()) != null) {
                target.putNextEntry(new ZipEntry(entry.getName()));
                source.transferTo(target);
                target.closeEntry();
            }

            for (int i = 0; i < PADDING_ENTRIES; i++) {
                target.putNextEntry(new ZipEntry("xl/padding" + i + ".xml"));
                for (int j = 0; j < PADDING_ENTRY_MEGABYTES; j++) {
                    target.write(chunk);
                }
                target.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private long paddingSize() {
        return (long)PADDING_ENTRIES * PADDING_ENTRY_MEGABYTES * 1024 * 1024;
    }

    // 엔트리당 크기와 압축률을 POI 기본 방어선(엔트리 10MB, 최소 압축률 0.01) 안쪽에 두어
    // 총합 검사가 없으면 통과하는 파일을 만든다
    private byte[] paddingChunk() {
        byte[] chunk = new byte[1024 * 1024];
        byte[] noise = new byte[40 * 1024];
        new Random(0).nextBytes(noise);
        System.arraycopy(noise, 0, chunk, 0, noise.length);
        return chunk;
    }
}
