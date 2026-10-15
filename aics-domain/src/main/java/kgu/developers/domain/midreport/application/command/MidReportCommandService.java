package kgu.developers.domain.midreport.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.midreport.exception.MidReportNotFoundException;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
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
    private final MilestoneRepository milestoneRepository;

    public MidReport getOrCreate(
        Long sectionId,
        Long teamId,
        Long milestoneId,
        String title,
        LocalDateTime dueDate,
        String topicTitle,
        String topicDescription
    ) {
        lockMilestone(sectionId, milestoneId);
        teamRepository.findByIdForUpdate(teamId).orElseThrow(TeamNotFoundException::new);
        return midReportRepository.findByTeamIdAndMilestoneId(teamId, milestoneId)
            .orElseGet(() -> midReportRepository.save(MidReport.create(
                teamId, milestoneId, title, dueDate, topicTitle, topicDescription
            )));
    }

    public MidReport updateBlock(
        Long sectionId,
        Long reportId,
        String blockKey,
        long expectedVersion,
        JsonNode fields,
        String editorId,
        LocalDateTime savedAt
    ) {
        MidReport report = get(reportId);
        lockMilestone(sectionId, report.getMilestoneId());
        report.updateBlock(blockKey, expectedVersion, fields, editorId, savedAt);
        return midReportRepository.save(report);
    }

    public MidReport completeBlock(
        Long sectionId,
        Long reportId,
        String blockKey,
        long expectedVersion,
        String editorId,
        LocalDateTime savedAt
    ) {
        MidReport report = get(reportId);
        lockMilestone(sectionId, report.getMilestoneId());
        report.completeBlock(blockKey, expectedVersion, editorId, savedAt);
        return midReportRepository.save(report);
    }

    public MidReport submit(
        Long sectionId,
        Long reportId,
        long expectedVersion,
        String submitterId,
        LocalDateTime submittedAt
    ) {
        MidReport report = get(reportId);
        lockMilestone(sectionId, report.getMilestoneId());
        report.submit(expectedVersion, submitterId, submittedAt);
        return midReportRepository.save(report);
    }

    public MidReport requestRevision(
        Long sectionId, Long reportId, java.util.List<String> affectedBlockKeys, LocalDateTime requestedAt
    ) {
        MidReport report = get(reportId);
        lockMilestone(sectionId, report.getMilestoneId());
        if (!report.requestRevision(affectedBlockKeys, requestedAt)) {
            return report;
        }
        return midReportRepository.save(report);
    }

    public MidReportRevisionCompletionResult completeRevision(
        Long sectionId,
        Long reportId,
        long expectedVersion,
        String professorId,
        LocalDateTime completedAt
    ) {
        MidReport report = get(reportId);
        lockMilestone(sectionId, report.getMilestoneId());
        if (!report.completeRevision(expectedVersion, professorId, completedAt)) {
            return new MidReportRevisionCompletionResult(report, false);
        }
        return new MidReportRevisionCompletionResult(midReportRepository.save(report), true);
    }

    private MidReport get(Long reportId) {
        return midReportRepository.findById(reportId).orElseThrow(MidReportNotFoundException::new);
    }

    // 삭제 중인 마일스톤과 같은 락을 잡아, 마일스톤 삭제와 동시에(또는 삭제 이후) 진행되는
    // 중간보고서 생성·수정·제출이 삭제된 마일스톤 아래에 고아 데이터를 남기지 않도록 한다.
    private void lockMilestone(Long sectionId, Long milestoneId) {
        milestoneRepository.findByIdAndSectionIdForUpdate(milestoneId, sectionId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
    }
}
