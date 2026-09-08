package kgu.developers.domain.midreport.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.midreport.exception.MidReportNotFoundException;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MidReportCommandService {
    private final MidReportRepository midReportRepository;
    private final TeamRepository teamRepository;

    public MidReport getOrCreate(
        Long teamId,
        Long milestoneId,
        String title,
        LocalDateTime dueDate,
        String topicTitle,
        String topicDescription
    ) {
        teamRepository.findByIdForUpdate(teamId).orElseThrow(TeamNotFoundException::new);
        return midReportRepository.findByTeamIdAndMilestoneId(teamId, milestoneId)
            .orElseGet(() -> midReportRepository.save(MidReport.create(
                teamId, milestoneId, title, dueDate, topicTitle, topicDescription
            )));
    }

    public MidReport updateBlock(
        Long reportId,
        String blockKey,
        long expectedVersion,
        JsonNode fields,
        String editorId,
        LocalDateTime savedAt
    ) {
        MidReport report = get(reportId);
        report.updateBlock(blockKey, expectedVersion, fields, editorId, savedAt);
        return midReportRepository.save(report);
    }

    public MidReport completeBlock(
        Long reportId,
        String blockKey,
        long expectedVersion,
        String editorId,
        LocalDateTime savedAt
    ) {
        MidReport report = get(reportId);
        report.completeBlock(blockKey, expectedVersion, editorId, savedAt);
        return midReportRepository.save(report);
    }

    private MidReport get(Long reportId) {
        return midReportRepository.findById(reportId).orElseThrow(MidReportNotFoundException::new);
    }
}
