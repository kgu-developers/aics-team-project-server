package kgu.developers.domain.midreport.infrastructure;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportBlock;
import kgu.developers.domain.midreport.domain.MidReportRevision;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "mid_report",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_mid_report_team_milestone",
        columnNames = {"team_id", "milestone_id"}
    )
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class MidReportJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "milestone_id", nullable = false)
    private Long milestoneId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Enumerated(STRING)
    @Column(nullable = false, length = 30)
    private MidReportStatus status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "submitted_by", length = 20)
    private String submittedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "revision_data", columnDefinition = "jsonb")
    private String revisionData;

    @Builder.Default
    @OneToMany(mappedBy = "midReport", cascade = ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<MidReportBlockJpaEntity> blocks = new ArrayList<>();

    static MidReportJpaEntity fromDomain(MidReport report) {
        MidReportJpaEntity entity = MidReportJpaEntity.builder()
            .id(report.getId())
            .version(report.getVersion())
            .teamId(report.getTeamId())
            .milestoneId(report.getMilestoneId())
            .title(report.getTitle())
            .dueDate(report.getDueDate())
            .status(report.getStatus())
            .submittedAt(report.getSubmittedAt())
            .submittedBy(report.getSubmittedBy())
            .revisionData(revisionData(report.getRevision()))
            .build();
        entity.blocks = report.getBlocks().stream()
            .map(block -> MidReportBlockJpaEntity.fromDomain(block, entity))
            .collect(Collectors.toCollection(ArrayList::new));
        return entity;
    }

    void updateFromDomain(MidReport report) {
        updatedAt = LocalDateTime.now();
        title = report.getTitle();
        dueDate = report.getDueDate();
        status = report.getStatus();
        submittedAt = report.getSubmittedAt();
        submittedBy = report.getSubmittedBy();
        revisionData = revisionData(report.getRevision());

        Map<String, MidReportBlockJpaEntity> entitiesByKey = blocks.stream()
            .collect(Collectors.toMap(MidReportBlockJpaEntity::getKey, Function.identity()));
        for (MidReportBlock block : report.getBlocks()) {
            MidReportBlockJpaEntity entity = entitiesByKey.get(block.getKey());
            if (entity == null) {
                blocks.add(MidReportBlockJpaEntity.fromDomain(block, this));
            } else {
                entity.updateFromDomain(block);
            }
        }
    }

    MidReport toDomain() {
        List<MidReportBlock> domainBlocks = blocks.stream()
            .map(MidReportBlockJpaEntity::toDomain)
            .sorted(Comparator.comparingInt(block -> blockOrder(block.getKey())))
            .toList();
        return MidReport.builder()
            .id(id)
            .version(version)
            .teamId(teamId)
            .milestoneId(milestoneId)
            .title(title)
            .dueDate(dueDate)
            .status(status)
            .submittedAt(submittedAt)
            .submittedBy(submittedBy)
            .revision(toRevision())
            .blocks(domainBlocks)
            .createdAt(getCreatedAt())
            .updatedAt(getUpdatedAt())
            .build();
    }

    private MidReportRevision toRevision() {
        if (revisionData == null) {
            return null;
        }
        JsonNode node = JsonConverter.parse(revisionData);
        return new MidReportRevision(
            strings(node.path("affectedBlockKeys")),
            strings(node.path("changedBlockKeys")),
            dateTime(node.path("requestedAt")),
            dateTime(node.path("resubmittedAt"))
        );
    }

    private static String revisionData(MidReportRevision revision) {
        if (revision == null) {
            return null;
        }
        return JsonConverter.toTree(Map.of(
            "affectedBlockKeys", revision.affectedBlockKeys(),
            "changedBlockKeys", revision.changedBlockKeys(),
            "requestedAt", revision.requestedAt().toString(),
            "resubmittedAt", revision.resubmittedAt() == null ? "" : revision.resubmittedAt().toString()
        )).toString();
    }

    private static List<String> strings(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(value.asText()));
        return values;
    }

    private static LocalDateTime dateTime(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank() ? LocalDateTime.parse(node.asText()) : null;
    }

    private static int blockOrder(String key) {
        return switch (key) {
            case "topic" -> 0;
            case "gui-design" -> 1;
            case "engine-design" -> 2;
            case "project-plan" -> 3;
            default -> Integer.MAX_VALUE;
        };
    }
}
