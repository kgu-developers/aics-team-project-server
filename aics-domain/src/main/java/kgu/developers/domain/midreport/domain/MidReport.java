package kgu.developers.domain.midreport.domain;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import kgu.developers.domain.midreport.exception.MidReportBlockIncompleteException;
import kgu.developers.domain.midreport.exception.MidReportSubmittedException;
import kgu.developers.domain.midreport.exception.MidReportVersionConflictException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class MidReport {
    private Long id;
    private Long teamId;
    private Long milestoneId;
    private String title;
    private Long version;
    private LocalDateTime dueDate;
    private MidReportStatus status;
    private LocalDateTime submittedAt;
    private String submittedBy;
    private MidReportRevision revision;
    private List<MidReportBlock> blocks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MidReport create(
        Long teamId,
        Long milestoneId,
        String title,
        LocalDateTime dueDate,
        String topicTitle,
        String topicDescription
    ) {
        List<MidReportBlock> blocks = Arrays.stream(MidReportBlockDefinition.values())
            .map(definition -> MidReportBlock.create(definition, definition.emptyFields(
                definition == MidReportBlockDefinition.TOPIC
                    ? Map.of("title", valueOrEmpty(topicTitle), "description", valueOrEmpty(topicDescription))
                    : Map.of()
            )))
            .toList();
        return MidReport.builder()
            .teamId(teamId)
            .milestoneId(milestoneId)
            .title(title)
            .dueDate(dueDate)
            .status(MidReportStatus.DRAFT)
            .blocks(blocks)
            .build();
    }

    public void updateBlock(String blockKey, long expectedVersion, com.fasterxml.jackson.databind.JsonNode fields,
                            String editorId, LocalDateTime savedAt) {
        MidReportBlock block = block(blockKey);
        validateMutable(expectedVersion);
        block.update(fields, editorId, savedAt);
    }

    public void completeBlock(String blockKey, long expectedVersion, String editorId, LocalDateTime savedAt) {
        MidReportBlock block = block(blockKey);
        validateMutable(expectedVersion);
        block.complete(editorId, savedAt);
    }

    public void submit(long expectedVersion, String submitterId, LocalDateTime submittedAt) {
        validateMutable(expectedVersion);
        boolean allBlocksCompleted = blocks.size() == MidReportBlockDefinition.values().length && blocks.stream()
            .allMatch(block -> block.getStatus() == MidReportBlockStatus.COMPLETED);
        if (!allBlocksCompleted) {
            throw new MidReportBlockIncompleteException();
        }
        this.status = MidReportStatus.SUBMITTED;
        this.submittedBy = submitterId;
        this.submittedAt = submittedAt;
    }

    public boolean requestRevision(List<String> affectedBlockKeys, LocalDateTime requestedAt) {
        if (status != MidReportStatus.SUBMITTED) {
            return false;
        }
        this.status = MidReportStatus.REVISION_REQUESTED;
        this.revision = new MidReportRevision(List.copyOf(affectedBlockKeys), List.of(), requestedAt, null);
        return true;
    }

    private void validateMutable(long expectedVersion) {
        long currentVersion = version == null ? 0L : version;
        if (currentVersion != expectedVersion) {
            throw new MidReportVersionConflictException();
        }
        if (status == MidReportStatus.SUBMITTED) {
            throw new MidReportSubmittedException();
        }
    }

    private MidReportBlock block(String blockKey) {
        return blocks.stream()
            .filter(block -> block.getKey().equals(blockKey))
            .findFirst()
            .orElseThrow(kgu.developers.domain.midreport.exception.MidReportBlockNotFoundException::new);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
