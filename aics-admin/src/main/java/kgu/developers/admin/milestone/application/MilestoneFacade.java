package kgu.developers.admin.milestone.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormResponse;
import kgu.developers.admin.milestone.presentation.request.MilestoneCreateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneEvaluationWindowRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneScheduleRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneStatusRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneUpdateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneWeekNumbersRequest;
import kgu.developers.admin.milestone.presentation.request.RequiredArtifactRequest;
import kgu.developers.admin.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.admin.milestone.presentation.response.MilestonePersistResponse;
import kgu.developers.admin.milestone.presentation.response.MilestoneResponse;
import kgu.developers.admin.milestone.presentation.response.MilestoneScheduleResponse;
import kgu.developers.admin.milestone.presentation.response.RequiredArtifactListResponse;
import kgu.developers.admin.milestone.presentation.response.RequiredArtifactPersistResponse;
import kgu.developers.domain.evaluation.application.command.PeerEvaluationFormCommandService;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import kgu.developers.domain.feedback.application.command.RequiredArtifactCommandService;
import kgu.developers.domain.feedback.application.query.RequiredArtifactQueryService;
import kgu.developers.domain.feedback.exception.InvalidRequiredArtifactRequestException;
import kgu.developers.domain.milestone.application.command.MilestoneCommandService;
import kgu.developers.domain.milestone.application.query.MilestoneQueryService;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.milestone.exception.InvalidMilestoneRequestException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MilestoneFacade {
    private final MilestoneCommandService milestoneCommandService;
    private final MilestoneQueryService milestoneQueryService;
    private final MilestoneAccessValidator milestoneAccessValidator;
    private final RequiredArtifactCommandService requiredArtifactCommandService;
    private final RequiredArtifactQueryService requiredArtifactQueryService;
    private final PeerEvaluationFormCommandService peerEvaluationFormCommandService;
    private final PeerEvaluationFormRepository peerEvaluationFormRepository;

    public MilestonePersistResponse createMilestone(
            Long sectionId,
            String professorId,
            MilestoneCreateRequest request
    ) {
        return asInvalidRequest(() -> {
            boolean isPeerEval = request.type() == MilestoneType.PEER_EVALUATION;
            MilestoneSchedule schedule = toSchedule(request.schedule(), isPeerEval);
            Long milestoneId = milestoneCommandService.createMilestone(
                    sectionId,
                    professorId,
                    request.title(),
                    request.description(),
                    request.weekNumber(),
                    schedule,
                    request.type(),
                    Boolean.TRUE.equals(request.allowResubmissionBeforeDueAt())
            );
            return MilestonePersistResponse.of(milestoneId);
        });
    }

    public MilestoneListResponse getMilestones(
            Long sectionId,
            String professorId,
            MilestoneStatus status
    ) {
        milestoneAccessValidator.validateSectionAccess(sectionId, professorId);
        return asInvalidRequest(() -> {
            List<Milestone> milestones = milestoneQueryService.getMilestones(sectionId, status);
            boolean hasPeerEval = milestones.stream().anyMatch(m -> m.getType() == MilestoneType.PEER_EVALUATION);
            if (!hasPeerEval) {
                return MilestoneListResponse.from(milestones);
            }
            Map<Long, PeerEvaluationForm> formsByMilestoneId = peerEvaluationFormRepository
                    .findAllBySectionIdOrderByIdDesc(sectionId).stream()
                    .collect(Collectors.toMap(
                            PeerEvaluationForm::getMilestoneId,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));
            List<MilestoneResponse> responses = milestones.stream()
                    .map(milestone -> toMilestoneResponse(milestone, formsByMilestoneId.get(milestone.getId())))
                    .toList();
            return new MilestoneListResponse(responses);
        });
    }

    public MilestoneResponse getMilestone(Long sectionId, String professorId, Long milestoneId) {
        milestoneAccessValidator.validateSectionAccess(sectionId, professorId);
        return asInvalidRequest(() -> {
            Milestone milestone = milestoneQueryService.getMilestone(sectionId, milestoneId);
            return toMilestoneResponse(milestone);
        });
    }

    @Transactional
    public void updateMilestone(
            Long sectionId,
            String professorId,
            Long milestoneId,
            MilestoneUpdateRequest request
    ) {
        asInvalidRequest(() -> {
            boolean isExplicitPeerEval = request.type() == MilestoneType.PEER_EVALUATION;
            boolean needsExisting = request.type() == null || isExplicitPeerEval;
            Milestone existing = needsExisting
                    ? milestoneQueryService.getMilestone(sectionId, milestoneId)
                    : null;
            MilestoneType effectiveType = request.type() != null
                    ? request.type()
                    : (existing != null ? existing.getType() : null);
            boolean isPeerEval = effectiveType == MilestoneType.PEER_EVALUATION;
            MilestoneSchedule existingSchedule = existing != null ? existing.getSchedule() : null;
            MilestoneSchedule schedule = toSchedule(request.schedule(), isPeerEval, existingSchedule);
            milestoneCommandService.updateMilestone(
                    sectionId,
                    professorId,
                    milestoneId,
                    request.title(),
                    request.description(),
                    schedule,
                    request.type(),
                    request.allowResubmissionBeforeDueAt()
            );
            if (isPeerEval) {
                LocalDateTime opensAt = schedule != null ? schedule.opensAt() : null;
                LocalDateTime closesAt = schedule != null ? schedule.dueAt() : null;
                if ((opensAt != null && closesAt != null) || request.anonymous() != null) {
                    peerEvaluationFormCommandService.updateFormByMilestoneId(
                            sectionId, milestoneId, request.anonymous(), opensAt, closesAt);
                }
            }
        });
    }

    public void changeStatus(
            Long sectionId,
            String professorId,
            Long milestoneId,
            MilestoneStatusRequest request
    ) {
        asInvalidRequest(() -> milestoneCommandService.changeStatus(
                sectionId,
                professorId,
                milestoneId,
                request.status()
        ));
    }

    public void updateEvaluationWindow(
            Long sectionId,
            String professorId,
            Long milestoneId,
            MilestoneEvaluationWindowRequest request
    ) {
        asInvalidRequest(() -> {
            milestoneCommandService.updateEvaluationWindow(
                    sectionId,
                    professorId,
                    milestoneId,
                    request.evaluationOpensAt(),
                    request.evaluationClosesAt()
            );
        });
    }

    public void updateWeekNumbers(
            Long sectionId,
            String professorId,
            MilestoneWeekNumbersRequest request
    ) {
        asInvalidRequest(() -> milestoneCommandService.updateWeekNumbers(
                sectionId,
                professorId,
                request.toDomain()
        ));
    }

    public RequiredArtifactListResponse getRequiredArtifacts(
            Long sectionId,
            String professorId,
            Long milestoneId
    ) {
        validateRequiredArtifactAccess(sectionId, professorId, milestoneId);
        return RequiredArtifactListResponse.from(
                requiredArtifactQueryService.getRequiredArtifacts(milestoneId)
        );
    }

    public RequiredArtifactPersistResponse createRequiredArtifact(
            Long sectionId,
            String professorId,
            Long milestoneId,
            RequiredArtifactRequest request
    ) {
        validateRequiredArtifactAccess(sectionId, professorId, milestoneId);
        return asInvalidRequiredArtifactRequest(() -> RequiredArtifactPersistResponse.of(
                requiredArtifactCommandService.create(
                        milestoneId,
                        request.type(),
                        request.label(),
                        request.required(),
                        request.allowedExtensionsValue(),
                        request.maxFileSizeMb()
                )
        ));
    }

    public void updateRequiredArtifact(
            Long sectionId,
            String professorId,
            Long milestoneId,
            Long requiredArtifactId,
            RequiredArtifactRequest request
    ) {
        validateRequiredArtifactAccess(sectionId, professorId, milestoneId);
        asInvalidRequiredArtifactRequest(() -> requiredArtifactCommandService.update(
                milestoneId,
                requiredArtifactId,
                request.type(),
                request.label(),
                request.required(),
                request.allowedExtensionsValue(),
                request.maxFileSizeMb()
        ));
    }

    public void deleteRequiredArtifact(
            Long sectionId,
            String professorId,
            Long milestoneId,
            Long requiredArtifactId
    ) {
        validateRequiredArtifactAccess(sectionId, professorId, milestoneId);
        requiredArtifactCommandService.delete(milestoneId, requiredArtifactId);
    }

    private void validateRequiredArtifactAccess(Long sectionId, String professorId, Long milestoneId) {
        milestoneAccessValidator.validateSectionAccess(sectionId, professorId);
        milestoneQueryService.getMilestone(sectionId, milestoneId);
    }

    private void asInvalidRequiredArtifactRequest(Runnable operation) {
        asInvalidRequiredArtifactRequest(() -> {
            operation.run();
            return null;
        });
    }

    private <T> T asInvalidRequiredArtifactRequest(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequiredArtifactRequestException(exception);
        }
    }

    private MilestoneResponse toMilestoneResponse(Milestone milestone) {
        if (milestone.getType() == MilestoneType.PEER_EVALUATION) {
            Optional<PeerEvaluationForm> formOpt = peerEvaluationFormRepository.findByMilestoneId(milestone.getId());
            return toMilestoneResponse(milestone, formOpt.orElse(null));
        }
        return MilestoneResponse.from(milestone);
    }

    private MilestoneResponse toMilestoneResponse(Milestone milestone, PeerEvaluationForm form) {
        if (milestone.getType() == MilestoneType.PEER_EVALUATION) {
            MilestoneSchedule s = milestone.getSchedule();
            LocalDateTime opensAt = s != null && s.opensAt() != null
                    ? s.opensAt()
                    : (form != null ? form.getOpensAt() : null);
            LocalDateTime dueAt = s != null && s.dueAt() != null
                    ? s.dueAt()
                    : (form != null ? form.getClosesAt() : null);
            LocalDateTime evalOpensAt = form != null ? form.getOpensAt() : opensAt;
            LocalDateTime evalClosesAt = form != null ? form.getClosesAt() : dueAt;
            MilestoneScheduleResponse scheduleResponse = new MilestoneScheduleResponse(
                    opensAt,
                    dueAt,
                    s != null ? s.lateSubmissionUntil() : null,
                    s != null ? s.revisionUntil() : null,
                    evalOpensAt,
                    evalClosesAt
            );
            return new MilestoneResponse(
                    milestone.getId(),
                    milestone.getSectionId(),
                    milestone.getTitle(),
                    milestone.getDescription(),
                    milestone.getWeekNumber(),
                    milestone.getStatus(),
                    scheduleResponse,
                    milestone.getType(),
                    milestone.isAllowResubmissionBeforeDueAt(),
                    PeerEvaluationFormResponse.from(form)
            );
        }
        return MilestoneResponse.from(milestone);
    }

    private MilestoneSchedule toSchedule(MilestoneScheduleRequest request) {
        return toSchedule(request, false, null);
    }

    private MilestoneSchedule toSchedule(MilestoneScheduleRequest request, boolean isPeerEval) {
        return toSchedule(request, isPeerEval, null);
    }

    private MilestoneSchedule toSchedule(
            MilestoneScheduleRequest request,
            boolean isPeerEval,
            MilestoneSchedule existingSchedule
    ) {
        if (request == null) {
            return null;
        }
        if (isPeerEval) {
            LocalDateTime existingOpensAt = existingSchedule != null ? existingSchedule.opensAt() : null;
            LocalDateTime existingDueAt = existingSchedule != null ? existingSchedule.dueAt() : null;

            LocalDateTime opensAt = request.evaluationOpensAt() != null
                    ? request.evaluationOpensAt()
                    : (request.opensAt() != null ? request.opensAt() : existingOpensAt);
            LocalDateTime dueAt = request.evaluationClosesAt() != null
                    ? request.evaluationClosesAt()
                    : (request.dueAt() != null ? request.dueAt() : existingDueAt);

            if (opensAt == null) {
                throw new IllegalArgumentException("상호평가 시작 시각은 필수입니다.");
            }
            if (dueAt == null) {
                throw new IllegalArgumentException("마감 시각은 필수입니다.");
            }
            if (!opensAt.isBefore(dueAt)) {
                throw new IllegalArgumentException("상호평가 시작 시각은 종료 시각보다 앞서야 합니다.");
            }

            return new MilestoneSchedule(
                    opensAt,
                    dueAt,
                    request.lateSubmissionUntil(),
                    request.revisionUntil(),
                    null,
                    null
            );
        }
        return request.toDomain();
    }

    private void asInvalidRequest(Runnable operation) {
        asInvalidRequest(() -> {
            operation.run();
            return null;
        });
    }

    private <T> T asInvalidRequest(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (IllegalArgumentException exception) {
            throw new InvalidMilestoneRequestException(exception);
        }
    }
}
