package kgu.developers.domain.midreport.domain;

import java.time.LocalDateTime;
import java.util.List;

public record MidReportRevision(
    List<String> affectedBlockKeys,
    List<String> changedBlockKeys,
    LocalDateTime requestedAt,
    LocalDateTime resubmittedAt
) {
}
