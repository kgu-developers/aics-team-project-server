package kgu.developers.domain.evaluation.domain;

import java.util.List;
import java.util.Optional;

public interface GradeRepository {
    Grade save(Grade grade);

    Optional<Grade> findById(Long id);

    Optional<Grade> findBySectionIdAndTeamIdAndUserId(Long sectionId, Long teamId, String userId);

    List<Grade> findAllBySectionIdAndTeamId(Long sectionId, Long teamId);
}
