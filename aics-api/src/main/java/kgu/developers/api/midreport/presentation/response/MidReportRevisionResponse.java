package kgu.developers.api.midreport.presentation.response;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.midreport.domain.MidReportRevision;

public record MidReportRevisionResponse(
    List<String> affectedBlockKeys,
    List<String> changedBlockKeys,
    LocalDateTime requestedAt,
    LocalDateTime resubmittedAt
) {
    static MidReportRevisionResponse from(MidReportRevision revision) {
        if (revision == null) {
            return null;
        }
        return new MidReportRevisionResponse(
            revision.affectedBlockKeys(),
            revision.changedBlockKeys(),
            revision.requestedAt(),
            revision.resubmittedAt()
        );
    }
}
