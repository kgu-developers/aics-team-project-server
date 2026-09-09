package kgu.developers.domain.midreport.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class MidReportBlock {
    private Long id;
    private String key;
    private JsonNode fields;
    private MidReportBlockStatus status;
    private String lastEditedBy;
    private LocalDateTime lastSavedAt;

    public static MidReportBlock create(MidReportBlockDefinition definition, JsonNode fields) {
        return MidReportBlock.builder()
            .key(definition.key())
            .fields(fields)
            .status(MidReportBlockStatus.IN_PROGRESS)
            .build();
    }

    public void update(JsonNode fields, String editorId, LocalDateTime savedAt) {
        MidReportBlockDefinition definition = MidReportBlockDefinition.fromKey(key);
        this.fields = definition.normalize(fields);
        this.status = MidReportBlockStatus.IN_PROGRESS;
        this.lastEditedBy = editorId;
        this.lastSavedAt = savedAt;
    }

    public void complete(String editorId, LocalDateTime savedAt) {
        MidReportBlockDefinition.fromKey(key).validateComplete(fields);
        this.status = MidReportBlockStatus.COMPLETED;
        this.lastEditedBy = editorId;
        this.lastSavedAt = savedAt;
    }
}
