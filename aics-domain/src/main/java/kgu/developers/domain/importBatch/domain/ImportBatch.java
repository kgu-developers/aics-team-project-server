package kgu.developers.domain.importBatch.domain;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.domain.importBatch.exception.ImportBatchAlreadyAppliedException;
import kgu.developers.domain.importBatch.exception.ImportBatchExpiredException;
import kgu.developers.domain.importBatch.exception.ImportBatchHasInvalidRowsException;
import lombok.*;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ImportBatch {
    private static final int MAX_FILE_NAME_LENGTH = 255;

    private Long id;
    private Long version;  // 낙관적 락 버전 (신규는 null)

    private String uploadedBy;  // 업로더 학번
    private Long sectionId;  // 분반 식별자

    private Type type;  // 유형
    private Status status;  // 상태
    private String fileName;  // 업로드 원본 파일명(기존 배치는 null)

    private JsonNode payload;  // 원본데이터 (형식 제약 없음)
    private JsonNode summary;  // 요약 (형식 제약 없음)

    private LocalDateTime expiredAt;  // 만료시각
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static ImportBatch create(String uploadedBy, Long sectionId, Type type,
                                     JsonNode payload, JsonNode summary, LocalDateTime expiredAt) {
        return create(uploadedBy, sectionId, type, null, payload, summary, expiredAt);
    }

    public static ImportBatch create(String uploadedBy, Long sectionId, Type type, String fileName,
                                     JsonNode payload, JsonNode summary, LocalDateTime expiredAt) {
        return ImportBatch.builder()
                .uploadedBy(requireNonNull(uploadedBy, "uploadedBy"))
                .sectionId(requireNonNull(sectionId, "sectionId"))
                .type(requireNonNull(type, "type"))
                .status(Status.PREVIEW)
                .fileName(normalizeFileName(fileName))
                .payload(requireNonNull(payload, "payload"))
                .summary(requireNonNull(summary, "summary"))
                .expiredAt(requireNonNull(expiredAt, "expiredAt"))
                .build();
    }

    private static String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String normalized = fileName.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        String baseName = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        baseName = baseName.strip();
        if (baseName.isBlank()) {
            return null;
        }
        return baseName.length() <= MAX_FILE_NAME_LENGTH
                ? baseName
                : baseName.substring(0, MAX_FILE_NAME_LENGTH);
    }

    public boolean hasErrors() {
        return summary.path("invalid").asInt(0) > 0;
    }

    public void apply(LocalDateTime now) {
        if (status == Status.APPLIED) {
            throw new ImportBatchAlreadyAppliedException();
        }
        if (isExpired(now)) {
            throw new ImportBatchExpiredException();
        }
        if (hasErrors()) {
            throw new ImportBatchHasInvalidRowsException();
        }
        this.status = Status.APPLIED;
    }

    public void expire() {
        this.status = Status.EXPIRED;
    }

    public boolean isExpired(LocalDateTime now) {
        return status == Status.EXPIRED || expiredAt.isBefore(now);
    }

    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }
}
