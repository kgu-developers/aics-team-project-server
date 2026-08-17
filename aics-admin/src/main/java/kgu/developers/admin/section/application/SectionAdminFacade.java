package kgu.developers.admin.section.application;

import kgu.developers.admin.section.presentation.request.SectionAdminRequest;
import kgu.developers.admin.section.presentation.request.SectionAdminUpdateRequest;
import kgu.developers.admin.section.presentation.request.SectionContactVisibilityUpdateRequest;
import kgu.developers.admin.section.presentation.response.SectionAdminListResponse;
import kgu.developers.admin.section.presentation.response.SectionAdminPersistResponse;
import kgu.developers.admin.section.presentation.response.SectionAdminResponse;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.section.application.command.SectionCommandService;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SectionAdminFacade {
    private final SectionCommandService sectionCommandService;
    private final SectionQueryService sectionQueryService;

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

    public SectionAdminResponse updateSectionContactVisibility(Long id, SectionContactVisibilityUpdateRequest request) {
        SectionDetail detail = sectionQueryService.getSectionById(id);
        sectionCommandService.changeContactVisiblePeriod(
            detail.section(),
            request.visibleFrom(),
            request.visibleUntil()
        );
        return SectionAdminResponse.from(detail);
    }

    public void deleteSection(Long id) {
        Section section = sectionQueryService.getSectionById(id).section();
        sectionCommandService.deleteSection(section);
    }
}
