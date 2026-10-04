package kgu.developers.domain.midreport.application.command;

import kgu.developers.domain.midreport.domain.MidReport;

public record MidReportRevisionCompletionResult(
    MidReport report,
    boolean completed
) {
}
