package kgu.developers.admin.milestone.application;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

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
import kgu.developers.admin.milestone.presentation.response.RequiredArtifactListResponse;
import kgu.developers.admin.milestone.presentation.response.RequiredArtifactPersistResponse;
import kgu.developers.domain.feedback.application.command.RequiredArtifactCommandService;
import kgu.developers.domain.feedback.application.query.RequiredArtifactQueryService;
import kgu.developers.domain.feedback.exception.InvalidRequiredArtifactRequestException;
import kgu.developers.domain.milestone.application.command.MilestoneCommandService;
import kgu.developers.domain.milestone.application.query.MilestoneQueryService;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
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

    public MilestonePersistResponse createMilestone(
            Long sectionId,
            String professorId,
            MilestoneCreateRequest request
    ) {
        return asInvalidRequest(() -> {
            Long milestoneId = milestoneCommandService.createMilestone(
                    sectionId,
                    professorId,
                    request.title(),
                    request.description(),
                    request.weekNumber(),
                    toSchedule(request.schedule()),
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
            return MilestoneListResponse.from(milestones);
        });
    }

    public MilestoneResponse getMilestone(Long sectionId, String professorId, Long milestoneId) {
        milestoneAccessValidator.validateSectionAccess(sectionId, professorId);
        return asInvalidRequest(() -> MilestoneResponse.from(
                milestoneQueryService.getMilestone(sectionId, milestoneId)
        ));
    }

    public void updateMilestone(
            Long sectionId,
            String professorId,
            Long milestoneId,
            MilestoneUpdateRequest request
    ) {
        asInvalidRequest(() -> milestoneCommandService.updateMilestone(
                sectionId,
                professorId,
                milestoneId,
                request.title(),
                request.description(),
                toSchedule(request.schedule()),
                request.type(),
                request.allowResubmissionBeforeDueAt()
        ));
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

    private MilestoneSchedule toSchedule(MilestoneScheduleRequest request) {
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
