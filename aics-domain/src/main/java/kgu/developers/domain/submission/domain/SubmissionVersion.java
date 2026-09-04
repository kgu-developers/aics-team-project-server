package kgu.developers.domain.submission.domain;

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
public class SubmissionVersion {
    private Long id;
    private Long submissionId;
    private int version;
    private String description;
    private String changeNote;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private boolean late;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static SubmissionVersion create(
            Long submissionId,
            int version,
            String description,
            String changeNote,
            String submittedBy,
            boolean late
    ) {
        return SubmissionVersion.builder()
                .submissionId(submissionId)
                .version(version)
                .description(description)
                .changeNote(changeNote)
                .submittedBy(submittedBy)
                .submittedAt(LocalDateTime.now())
                .late(late)
                .build();
    }
}
