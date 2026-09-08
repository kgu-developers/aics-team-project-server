package kgu.developers.api.midreport.application;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kgu.developers.api.midreport.presentation.request.MidReportBlockCompletionRequest;
import kgu.developers.api.midreport.presentation.request.MidReportBlockUpdateRequest;
import kgu.developers.api.midreport.presentation.request.MidReportSubmissionRequest;
import kgu.developers.api.midreport.presentation.response.MidReportResponse;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.midreport.application.command.MidReportCommandService;
import kgu.developers.domain.midreport.application.query.MidReportQueryService;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.exception.MidReportNotFoundException;
import kgu.developers.domain.midreport.exception.MidReportLeaderOnlyException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class MidReportFacade {
    private final MidReportCommandService midReportCommandService;
    private final MidReportQueryService midReportQueryService;
    private final TeamAccessValidator teamAccessValidator;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final UserQueryService userQueryService;

    public MidReportResponse getCurrent(String userId) {
        CurrentContext context = currentContext(userId);
        Project project = projectRepository.findAllByTeamId(context.team().getId()).stream().findFirst().orElse(null);
        String baseTitle = project == null ? context.team().getName() : project.getTitle();
        MidReport report = midReportCommandService.getOrCreate(
            context.team().getId(),
            context.milestone().getId(),
            baseTitle + " 중간보고서",
            context.milestone().getSchedule().dueAt(),
            project == null ? "" : project.getTitle(),
            project == null ? "" : project.getDescription()
        );
        return response(report);
    }

    public MidReportResponse updateBlock(
        Long reportId,
        String blockKey,
        String userId,
        MidReportBlockUpdateRequest request
    ) {
        MidReport current = authorizeActiveStudent(reportId, userId);
        MidReport saved = midReportCommandService.updateBlock(
            current.getId(), blockKey, request.version(), request.fields(), userId, LocalDateTime.now()
        );
        return response(saved);
    }

    public MidReportResponse completeBlock(
        Long reportId,
        String blockKey,
        String userId,
        MidReportBlockCompletionRequest request
    ) {
        MidReport current = authorizeActiveStudent(reportId, userId);
        MidReport saved = midReportCommandService.completeBlock(
            current.getId(), blockKey, request.version(), userId, LocalDateTime.now()
        );
        return response(saved);
    }

    public MidReportResponse submit(Long reportId, String userId, MidReportSubmissionRequest request) {
        MidReport current = authorizeActiveStudent(reportId, userId);
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(current.getTeamId(), userId)
            .orElseThrow(MidReportLeaderOnlyException::new);
        if (!member.isLeader()) {
            throw new MidReportLeaderOnlyException();
        }
        MidReport saved = midReportCommandService.submit(
            current.getId(), request.version(), userId, LocalDateTime.now()
        );
        return response(saved);
    }

    private CurrentContext currentContext(String userId) {
        validateUserRole(userId);
        boolean hasActiveTeam = false;
        for (Enrollment enrollment : enrollmentRepository.findAllByUserId(userId)) {
            if (!enrollment.isActiveStudent()) {
                continue;
            }
            TeamMember member = teamMemberRepository.findActiveBySectionIdAndUserId(enrollment.getSectionId(), userId)
                .orElse(null);
            if (member == null) {
                continue;
            }
            hasActiveTeam = true;
            Milestone milestone = milestoneRepository.findAllBySectionIdOrderByWeekNumber(enrollment.getSectionId()).stream()
                .filter(item -> item.getStatus() == MilestoneStatus.PUBLISHED)
                .filter(item -> item.getType() == MilestoneType.MID_REPORT)
                .reduce((first, second) -> second)
                .orElse(null);
            if (milestone != null) {
                Team team = teamRepository.findById(member.getTeamId()).orElseThrow(MidReportNotFoundException::new);
                teamAccessValidator.validateMembership(team.getId(), userId);
                return new CurrentContext(team, milestone);
            }
        }
        if (hasActiveTeam) {
            throw new MidReportNotFoundException();
        }
        throw new AccessDeniedException("활성 학생으로 소속된 팀의 중간보고서만 접근할 수 있습니다.");
    }

    private MidReport authorizeActiveStudent(Long reportId, String userId) {
        validateUserRole(userId);
        MidReport report = midReportQueryService.getById(reportId);
        teamAccessValidator.validateMembership(report.getTeamId(), userId);
        Team team = teamRepository.findById(report.getTeamId()).orElseThrow(MidReportNotFoundException::new);
        boolean activeStudent = enrollmentRepository.findBySectionIdAndUserId(team.getSectionId(), userId)
            .map(Enrollment::isActiveStudent)
            .orElse(false);
        if (!activeStudent) {
            throw new AccessDeniedException("활성 학생으로 소속된 팀의 중간보고서만 수정할 수 있습니다.");
        }
        return report;
    }

    private void validateUserRole(String userId) {
        User user = userQueryService.getUserByStudentNumber(userId);
        if (user.getGlobalRole() != UserGlobalRole.USER) {
            throw new AccessDeniedException("일반 사용자 중 활성 학생만 중간보고서에 접근할 수 있습니다.");
        }
    }

    private MidReportResponse response(MidReport report) {
        String leaderId = teamMemberRepository.findLeaderByTeamId(report.getTeamId())
            .map(TeamMember::getUserId)
            .orElse(null);
        Set<String> userIds = report.getBlocks().stream()
            .map(block -> block.getLastEditedBy())
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (leaderId != null) {
            userIds.add(leaderId);
        }
        if (report.getSubmittedBy() != null) {
            userIds.add(report.getSubmittedBy());
        }
        Map<String, String> names = userQueryService.getUsersByStudentNumbersIncludingDeleted(List.copyOf(userIds)).stream()
            .collect(Collectors.toMap(User::getStudentNumber, User::getName, (first, second) -> first));
        LocalDateTime currentDueDate = milestoneRepository.findById(report.getMilestoneId())
            .map(milestone -> milestone.getSchedule().dueAt())
            .orElse(report.getDueDate());
        return MidReportResponse.from(
            report,
            currentDueDate,
            names.get(leaderId),
            names.get(report.getSubmittedBy()),
            names
        );
    }

    private record CurrentContext(Team team, Milestone milestone) {
    }
}
