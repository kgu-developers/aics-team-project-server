package kgu.developers.admin.section.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.section.application.SectionAdminFacade;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sections")
public class SectionAdminControllerImpl implements SectionAdminController {

    private final SectionAdminFacade sectionAdminFacade;

    @Override
    @PostMapping
    public ResponseEntity<SectionAdminPersistResponse> createSection(
        @Valid @RequestBody SectionAdminRequest request) {
        SectionAdminPersistResponse response = sectionAdminFacade.createSection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/{sectionId}")
    public ResponseEntity<SectionAdminResponse> getSectionById(
            @Positive @PathVariable Long sectionId) {
        SectionAdminResponse response = sectionAdminFacade.getSectionsById(sectionId);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping(params = {"courseId", "!professorId"})
    public ResponseEntity<SectionAdminListResponse> getSectionsByCourseId(
        @Positive @RequestParam Long courseId) {
        SectionAdminListResponse response = sectionAdminFacade.getSectionsByCourseId(courseId);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping(params = {"professorId", "!courseId"})
    public ResponseEntity<SectionAdminListResponse> getSectionsByProfessorId(
        @NotBlank @RequestParam String professorId,
        @RequestParam(required = false) StatusType status,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) SemesterType semester) {
        SectionAdminListResponse response =
            sectionAdminFacade.getSectionsByProfessorId(professorId, status, year, semester);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/{sectionId}/enrollments")
    public ResponseEntity<EnrollmentAdminPersistResponse> createEnrollment(
        @Positive @PathVariable Long sectionId,
        @Valid @RequestBody EnrollmentAdminRequest request) {
        EnrollmentAdminPersistResponse response = sectionAdminFacade.createEnrollment(sectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/{sectionId}/enrollments")
    public ResponseEntity<EnrollmentAdminListResponse> getEnrollmentsBySectionId(
        @Positive @PathVariable Long sectionId) {
        EnrollmentAdminListResponse response = sectionAdminFacade.getEnrollmentsBySectionId(sectionId);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{sectionId}/teams")
    public ResponseEntity<TeamAdminListResponse> getTeamsBySectionId(
        @Positive @PathVariable Long sectionId) {
        TeamAdminListResponse response = sectionAdminFacade.getTeamsBySectionId(sectionId);
        return ResponseEntity.ok(response);
    }

    @Override
    @PatchMapping("/{sectionId}/enrollments/{studentNumber}")
    public ResponseEntity<EnrollmentAdminResponse> updateEnrollment(
        @Positive @PathVariable Long sectionId,
        @NotBlank @PathVariable String studentNumber,
        @Valid @RequestBody EnrollmentAdminUpdateRequest request) {
        EnrollmentAdminResponse response = sectionAdminFacade.updateEnrollment(sectionId, studentNumber, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PatchMapping("/{sectionId}")
    public ResponseEntity<SectionAdminResponse> updateSection(
        @Positive @PathVariable Long sectionId,
        @Valid @RequestBody SectionAdminUpdateRequest request) {
        SectionAdminResponse response = sectionAdminFacade.updateSection(sectionId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PatchMapping("/{sectionId}/contact-visibility")
    public ResponseEntity<SectionAdminResponse> updateSectionContactVisibility(
            @Positive @PathVariable Long sectionId,
            @Valid @RequestBody SectionContactVisibilityUpdateRequest request) {
        SectionAdminResponse response = sectionAdminFacade.updateSectionContactVisibility(sectionId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{sectionId}")
    public ResponseEntity<Void> deleteSection(
        @Positive @PathVariable Long sectionId) {
        
        sectionAdminFacade.deleteSection(sectionId);
        return ResponseEntity.noContent().build();
    }
}
