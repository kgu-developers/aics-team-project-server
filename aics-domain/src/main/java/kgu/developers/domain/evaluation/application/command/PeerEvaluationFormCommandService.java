package kgu.developers.domain.evaluation.application.command;

import java.time.LocalDateTime;
import java.util.Optional;

import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import kgu.developers.domain.evaluation.exception.PeerEvaluationFormAlreadyExistsException;
import kgu.developers.domain.evaluation.exception.PeerEvaluationFormNotFoundException;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PeerEvaluationFormCommandService {

    private final PeerEvaluationFormRepository formRepository;
    private final MilestoneRepository milestoneRepository;

    public Long createForm(
            Long sectionId,
            Long milestoneId,
            boolean anonymous,
            LocalDateTime opensAt,
            LocalDateTime closesAt
    ) {
        if (sectionId == null || sectionId <= 0) {
            throw new IllegalArgumentException("분반 식별자는 양수여야 합니다.");
        }
        if (milestoneId == null || milestoneId <= 0) {
            throw new IllegalArgumentException("마일스톤 식별자는 양수여야 합니다.");
        }
        milestoneRepository.findByIdAndSectionIdForUpdate(milestoneId, sectionId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        if (formRepository.findByMilestoneId(milestoneId).isPresent()) {
            throw new PeerEvaluationFormAlreadyExistsException();
        }
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
        updateFormByMilestoneId(sectionId, milestoneId, null, opensAt, closesAt);
    }

    public void updateFormByMilestoneId(
            Long sectionId,
            Long milestoneId,
            Boolean anonymous,
            LocalDateTime opensAt,
            LocalDateTime closesAt
    ) {
        Optional<PeerEvaluationForm> formOpt = formRepository.findByMilestoneId(milestoneId);
        if (formOpt.isPresent()) {
            PeerEvaluationForm form = formOpt.get();
            if (sectionId != null && !form.getSectionId().equals(sectionId)) {
                throw new PeerEvaluationFormNotFoundException();
            }
            boolean newAnonymous = anonymous != null ? anonymous : form.isAnonymous();
            LocalDateTime newOpensAt = opensAt != null ? opensAt : form.getOpensAt();
            LocalDateTime newClosesAt = closesAt != null ? closesAt : form.getClosesAt();
            form.update(newAnonymous, newOpensAt, newClosesAt);
            formRepository.save(form);
        } else if (sectionId != null && opensAt != null && closesAt != null) {
            boolean isAnonymous = anonymous != null ? anonymous : true;
            PeerEvaluationForm form = PeerEvaluationForm.create(
                    sectionId, milestoneId, isAnonymous, opensAt, closesAt);
            formRepository.save(form);
        } else if (sectionId != null) {
            throw new PeerEvaluationFormNotFoundException();
        }
    }
}
