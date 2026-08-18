package kgu.developers.admin.section.application;

import kgu.developers.admin.enrollment.presentation.request.EnrollmentAdminRequest;
import kgu.developers.admin.enrollment.presentation.request.EnrollmentAdminUpdateRequest;
import kgu.developers.admin.section.presentation.request.SectionAdminRequest;
import kgu.developers.admin.section.presentation.request.SectionAdminUpdateRequest;
import kgu.developers.admin.section.presentation.request.SectionContactVisibilityUpdateRequest;
import kgu.developers.admin.enrollment.presentation.response.EnrollmentAdminListResponse;
import kgu.developers.admin.enrollment.presentation.response.EnrollmentAdminPersistResponse;
import kgu.developers.admin.enrollment.presentation.response.EnrollmentAdminResponse;
import kgu.developers.admin.section.presentation.response.SectionAdminListResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import kgu.developers.admin.section.presentation.response.SectionAdminPersistResponse;
import kgu.developers.admin.section.presentation.response.SectionAdminResponse;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.enrollment.application.command.EnrollmentCommandService;
import kgu.developers.domain.enrollment.application.query.EnrollmentQueryService;
import kgu.developers.domain.section.application.command.SectionCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SectionAdminFacade {
    private final SectionCommandService sectionCommandService;
    private final SectionQueryService sectionQueryService;
    private final EnrollmentQueryService enrollmentQueryService;
    private final EnrollmentCommandService enrollmentCommandService;
    private final TeamQueryService teamQueryService;

    public SectionAdminPersistResponse createSection(SectionAdminRequest request) {
        Long id = sectionCommandService.createSection(
            request.professorId(),
            request.courseId(),
            request.code(),
            request.name(),
            request.classTime(),
            request.capacity(),
            request.contactVisibleFrom(),
            request.contactVisibleUntil()
        );
        return SectionAdminPersistResponse.of(id);
    }

    public SectionAdminResponse getSectionsById(Long id) {
        return SectionAdminResponse.from(sectionQueryService.getSectionById(id));
    }

    public SectionAdminListResponse getSectionsByCourseId(Long courseId) {
        return SectionAdminListResponse.from(sectionQueryService.getSectionsByCourseId(courseId));
    }

    public SectionAdminListResponse getSectionsByProfessorId(String professorId, StatusType status, Integer year,
                                                             SemesterType semester) {
        return SectionAdminListResponse.from(
            sectionQueryService.getSectionsByProfessorId(professorId, status, year, semester));
    }

    public TeamAdminListResponse getTeamsBySectionId(Long sectionId) {
        return TeamAdminListResponse.from(teamQueryService.getTeamsBySectionId(sectionId));
    }

    @Transactional
    public SectionAdminResponse updateSection(Long id, SectionAdminUpdateRequest request) {
        SectionDetail detail = sectionQueryService.getSectionById(id);
        sectionCommandService.updateSection(
            detail.section(),
            request.professorId(),
            request.courseId(),
            request.code(),
            request.name(),
            request.classTime(),
            request.capacity(),
            null,
            null
        );
        return SectionAdminResponse.from(sectionQueryService.getSectionById(id));
    }

    @Transactional
    public SectionAdminResponse updateSectionContactVisibility(Long id, SectionContactVisibilityUpdateRequest request) {
        SectionDetail detail = sectionQueryService.getSectionById(id);
        sectionCommandService.changeContactVisiblePeriod(
            detail.section(),
            request.visibleFrom(),
            request.visibleUntil()
        );
        return SectionAdminResponse.from(detail);
    }

    @Transactional
    public void deleteSection(Long id) {
        Section section = sectionQueryService.getSectionById(id).section();
        sectionCommandService.deleteSection(section);
    }

    public EnrollmentAdminPersistResponse createEnrollment(Long sectionId, EnrollmentAdminRequest request) {
        Long id = enrollmentCommandService.createEnrollment(sectionId, request.studentNumber(), request.role());
        return EnrollmentAdminPersistResponse.of(id);
    }

    @Transactional
    public EnrollmentAdminResponse updateEnrollment(Long sectionId, String studentNumber,
                                                    EnrollmentAdminUpdateRequest request) {
        enrollmentCommandService.updateEnrollment(sectionId, studentNumber, request.role(), request.status());
        return EnrollmentAdminResponse.from(enrollmentQueryService.getEnrollment(sectionId, studentNumber));
    }

    public EnrollmentAdminListResponse getEnrollmentsBySectionId(Long sectionId) {
        return EnrollmentAdminListResponse.from(enrollmentQueryService.getEnrollmentsBySectionId(sectionId));
    }
}
