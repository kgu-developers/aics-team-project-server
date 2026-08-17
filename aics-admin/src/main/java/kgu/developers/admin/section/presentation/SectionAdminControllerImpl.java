package kgu.developers.admin.section.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.section.application.SectionAdminFacade;
import kgu.developers.admin.section.presentation.request.SectionAdminRequest;
import kgu.developers.admin.section.presentation.request.SectionAdminUpdateRequest;
import kgu.developers.admin.section.presentation.request.SectionContactVisibilityUpdateRequest;
import kgu.developers.admin.section.presentation.response.SectionAdminListResponse;
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
@RequestMapping("/api/v1/admin/oop/sections")
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
    @GetMapping(params = "courseId")
    public ResponseEntity<SectionAdminListResponse> getSectionsByCourseId(
        @Positive @RequestParam Long courseId) {
        SectionAdminListResponse response = sectionAdminFacade.getSectionsByCourseId(courseId);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping(params = "professorId")
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
