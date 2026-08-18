package kgu.developers.domain.importBatch.domain;

import kgu.developers.domain.importBatch.exception.ImportBatchAlreadyAppliedException;
import kgu.developers.domain.importBatch.exception.ImportBatchExpiredException;
import kgu.developers.domain.importBatch.exception.ImportBatchHasInvalidRowsException;
import lombok.*;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ImportBatch {
    private Long id;
    private Long version;  // 낙관적 락 버전 (신규는 null)

    private String uploadedBy;  // 업로더 학번
    private Long sectionId;  // 분반 식별자

    private Type type;  // 유형
    private Status status;  // 상태

    private ImportPayload payload;  // 원본데이터
    private ImportSummary summary;  // 요약

    private LocalDateTime expiredAt;  // 만료시각
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static ImportBatch create(String uploadedBy, Long sectionId, Type type,
                                     ImportPayload payload, LocalDateTime expiredAt) {
        return ImportBatch.builder()
                .uploadedBy(requireNonNull(uploadedBy, "uploadedBy"))
                .sectionId(requireNonNull(sectionId, "sectionId"))
                .type(requireNonNull(type, "type"))
                .status(Status.PREVIEW)
                .payload(requireNonNull(payload, "payload"))
                .summary(ImportSummary.of(payload))
                .expiredAt(requireNonNull(expiredAt, "expiredAt"))
                .build();
    }

    public List<ImportRow> getApplicableRows() {
        return payload.applicableRows();
    }

    public String cell(ImportRow row, String column) {
        return payload.cell(row, column);
    }

    public boolean hasErrors() {
        return summary.invalid() > 0;
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
