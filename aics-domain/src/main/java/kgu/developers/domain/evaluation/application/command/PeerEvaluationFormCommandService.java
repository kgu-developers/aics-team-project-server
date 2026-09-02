package kgu.developers.domain.evaluation.application.command;

import java.time.LocalDateTime;

import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PeerEvaluationFormCommandService {

    private final PeerEvaluationFormRepository formRepository;

    public Long createForm(
            Long sectionId,
            Long milestoneId,
            boolean anonymous,
            LocalDateTime opensAt,
            LocalDateTime closesAt
    ) {
        PeerEvaluationForm form = PeerEvaluationForm.create(
                sectionId, milestoneId, anonymous, opensAt, closesAt);
        return formRepository.save(form).getId();
    }
}
