package kgu.developers.domain.evaluation.application.command;

import java.time.LocalDateTime;
import java.util.Optional;

import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import kgu.developers.domain.evaluation.exception.PeerEvaluationFormNotFoundException;
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

    public void updateForm(
            Long sectionId,
            Long formId,
            boolean anonymous,
            LocalDateTime opensAt,
            LocalDateTime closesAt
    ) {
        PeerEvaluationForm form = formRepository.findById(formId)
                .orElseThrow(PeerEvaluationFormNotFoundException::new);
        if (!form.getSectionId().equals(sectionId)) {
            throw new PeerEvaluationFormNotFoundException();
        }
        form.update(anonymous, opensAt, closesAt);
        formRepository.save(form);
    }

    public void updateFormPeriodByMilestoneId(
            Long sectionId,
            Long milestoneId,
            LocalDateTime opensAt,
            LocalDateTime closesAt
    ) {
        Optional<PeerEvaluationForm> formOpt = formRepository.findByMilestoneId(milestoneId);
        if (formOpt.isPresent()) {
            PeerEvaluationForm form = formOpt.get();
            form.updatePeriod(opensAt, closesAt);
            formRepository.save(form);
        } else if (sectionId != null && opensAt != null && closesAt != null) {
            PeerEvaluationForm form = PeerEvaluationForm.create(
                    sectionId, milestoneId, true, opensAt, closesAt);
            formRepository.save(form);
        }
    }
}
