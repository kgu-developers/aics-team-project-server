package kgu.developers.domain.midreport.infrastructure;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.midreport.domain.MidReportBlock;
import kgu.developers.domain.midreport.domain.MidReportBlockStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "mid_report_block",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_mid_report_block_report_key",
        columnNames = {"mid_report_id", "block_key"}
    )
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class MidReportBlockJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "mid_report_id", nullable = false)
    private MidReportJpaEntity midReport;

    @Column(name = "block_key", nullable = false, length = 30)
    private String key;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String fields;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private MidReportBlockStatus status;

    @Column(name = "last_edited_by", length = 20)
    private String lastEditedBy;

    @Column(name = "last_saved_at")
    private LocalDateTime lastSavedAt;

    static MidReportBlockJpaEntity fromDomain(MidReportBlock block, MidReportJpaEntity report) {
        return MidReportBlockJpaEntity.builder()
            .id(block.getId())
            .midReport(report)
            .key(block.getKey())
            .fields(block.getFields().toString())
            .status(block.getStatus())
            .lastEditedBy(block.getLastEditedBy())
            .lastSavedAt(block.getLastSavedAt())
            .build();
    }

    void updateFromDomain(MidReportBlock block) {
        fields = block.getFields().toString();
        status = block.getStatus();
        lastEditedBy = block.getLastEditedBy();
        lastSavedAt = block.getLastSavedAt();
    }

    MidReportBlock toDomain() {
        return MidReportBlock.builder()
            .id(id)
            .key(key)
            .fields(JsonConverter.parse(fields))
            .status(status)
            .lastEditedBy(lastEditedBy)
            .lastSavedAt(lastSavedAt)
            .build();
    }
}
