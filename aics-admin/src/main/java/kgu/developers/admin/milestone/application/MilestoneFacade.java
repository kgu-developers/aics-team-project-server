package kgu.developers.admin.milestone.application;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import kgu.developers.admin.milestone.presentation.request.MilestoneCreateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneEvaluationWindowRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneScheduleRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneStatusRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneUpdateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneWeekNumbersRequest;
import kgu.developers.admin.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.admin.milestone.presentation.response.MilestonePersistResponse;
import kgu.developers.admin.milestone.presentation.response.MilestoneResponse;
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

    public MilestonePersistResponse createMilestone(Long sectionId, MilestoneCreateRequest request) {
        return asInvalidRequest(() -> {
            Long milestoneId = milestoneCommandService.createMilestone(
                    sectionId,
                    request.title(),
                    request.description(),
                    request.weekNumber(),
                    toSchedule(request.schedule())
            );
            return MilestonePersistResponse.of(milestoneId);
        });
    }

    public MilestoneListResponse getMilestones(Long sectionId, MilestoneStatus status) {
        return asInvalidRequest(() -> {
            List<Milestone> milestones = milestoneQueryService.getMilestones(sectionId, status);
            return MilestoneListResponse.from(milestones);
        });
    }

    public MilestoneResponse getMilestone(Long sectionId, Long milestoneId) {
        return asInvalidRequest(() -> MilestoneResponse.from(
                milestoneQueryService.getMilestone(sectionId, milestoneId)
        ));
    }

    public void updateMilestone(Long sectionId, Long milestoneId, MilestoneUpdateRequest request) {
        asInvalidRequest(() -> milestoneCommandService.updateMilestone(
                sectionId,
                milestoneId,
                request.title(),
                request.description(),
                toSchedule(request.schedule())
        ));
    }

    public void changeStatus(Long sectionId, Long milestoneId, MilestoneStatusRequest request) {
        asInvalidRequest(() -> milestoneCommandService.changeStatus(
                sectionId,
                milestoneId,
                request.status()
        ));
    }

    public void updateEvaluationWindow(
            Long sectionId,
            Long milestoneId,
            MilestoneEvaluationWindowRequest request
    ) {
        asInvalidRequest(() -> {
            request.validateIntent();
            milestoneCommandService.updateEvaluationWindow(
                    sectionId,
                    milestoneId,
                    request.evaluationOpensAt(),
                    request.evaluationClosesAt()
            );
        });
    }

    public void updateWeekNumbers(Long sectionId, MilestoneWeekNumbersRequest request) {
        asInvalidRequest(() -> milestoneCommandService.updateWeekNumbers(
                sectionId,
                request.toDomain()
        ));
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
        } catch (IllegalArgumentException | DataIntegrityViolationException exception) {
            throw new InvalidMilestoneRequestException(exception);
        }
    }
}
