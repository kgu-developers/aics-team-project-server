package kgu.developers.api.section.presentation;

import jakarta.validation.constraints.Positive;
import kgu.developers.api.section.application.SectionFacade;
import kgu.developers.api.section.presentation.response.SectionListResponse;
import kgu.developers.api.section.presentation.response.SectionResponse;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/sections")
public class SectionControllerImpl implements SectionController {

    private final SectionFacade sectionFacade;

    @Override
    @GetMapping
    public ResponseEntity<SectionListResponse> getMySections(
        @RequestParam(required = false) StatusType status,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) SemesterType semester) {
        String studentNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(sectionFacade.getMySections(studentNumber, status, year, semester));
    }

    @Override
    @GetMapping("/{sectionId}")
    public ResponseEntity<SectionResponse> getSectionById(
        @Positive @PathVariable Long sectionId) {
        return ResponseEntity.ok(sectionFacade.getSectionById(sectionId));
    }
}
