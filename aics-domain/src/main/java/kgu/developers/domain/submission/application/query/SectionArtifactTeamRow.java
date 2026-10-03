package kgu.developers.domain.submission.application.query;

import java.util.List;

public record SectionArtifactTeamRow(
        Long teamId,
        String teamName,
        List<SectionArtifactMember> members,
        long meetingRecordCount,
        long meetingRecordEditCount,
        List<SectionArtifactStageRow> stages
) {

    public long submittedStageCount() {
        return stages.stream()
                .filter(stage -> stage.firstSubmittedAt() != null)
                .count();
    }

    public long overdueMissingStageCount() {
        return stages.stream()
                .filter(SectionArtifactStageRow::overdueMissing)
                .count();
    }
}
