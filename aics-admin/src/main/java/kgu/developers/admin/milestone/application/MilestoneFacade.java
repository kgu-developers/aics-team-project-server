package kgu.developers.admin.milestone.application;

import java.util.List;

import org.springframework.stereotype.Component;

import kgu.developers.admin.milestone.presentation.request.MilestoneCreateRequest;
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
        Long milestoneId = milestoneCommandService.createMilestone(
                sectionId,
                request.title(),
                request.description(),
                request.weekNumber(),
                toSchedule(request.schedule())
        );
        return MilestonePersistResponse.of(milestoneId);
    }

    public MilestoneListResponse getMilestones(Long sectionId, MilestoneStatus status) {
        List<Milestone> milestones = milestoneQueryService.getMilestones(sectionId, status);
        return MilestoneListResponse.from(milestones);
    }

    public MilestoneResponse getMilestone(Long sectionId, Long milestoneId) {
        return MilestoneResponse.from(milestoneQueryService.getMilestone(sectionId, milestoneId));
    }

    public void updateMilestone(Long sectionId, Long milestoneId, MilestoneUpdateRequest request) {
        milestoneCommandService.updateMilestone(
                sectionId,
                milestoneId,
                request.title(),
                request.description(),
                toSchedule(request.schedule())
        );
    }

    public void changeStatus(Long sectionId, Long milestoneId, MilestoneStatusRequest request) {
        milestoneCommandService.changeStatus(sectionId, milestoneId, request.status());
    }

    public void updateWeekNumbers(Long sectionId, MilestoneWeekNumbersRequest request) {
        try {
            milestoneCommandService.updateWeekNumbers(sectionId, request.toDomain());
        } catch (IllegalArgumentException exception) {
            throw new InvalidMilestoneRequestException(exception);
        }
    }

    private MilestoneSchedule toSchedule(MilestoneScheduleRequest request) {
        try {
            return request.toDomain();
        } catch (IllegalArgumentException exception) {
            throw new InvalidMilestoneRequestException(exception);
        }
    }
}
