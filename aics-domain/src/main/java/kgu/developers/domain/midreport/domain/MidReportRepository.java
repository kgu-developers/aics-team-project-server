package kgu.developers.domain.midreport.domain;

import java.util.Optional;

public interface MidReportRepository {
    MidReport save(MidReport midReport);

    Optional<MidReport> findById(Long id);

    Optional<MidReport> findByTeamIdAndMilestoneId(Long teamId, Long milestoneId);
}
