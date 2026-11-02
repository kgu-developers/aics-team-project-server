package kgu.developers.domain.teammessage.domain;

import java.util.List;
import java.util.Map;

public interface DocumentFeedbackRepository {
    Map<Long, DocumentFeedbackSummary> summarizeFeedback(
        List<Long> teamIds, TeamMessageRelatedType relatedType, List<Long> relatedIds
    );
}
