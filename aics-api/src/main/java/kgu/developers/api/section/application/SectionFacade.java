package kgu.developers.api.section.application;

import org.springframework.stereotype.Component;

import kgu.developers.api.section.presentation.response.SectionListResponse;
import kgu.developers.api.section.presentation.response.SectionResponse;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.section.application.query.SectionQueryService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SectionFacade {
    private final SectionQueryService sectionQueryService;

    public SectionResponse getSectionById(Long id) {
        return SectionResponse.from(sectionQueryService.getSectionById(id));
    }

    public SectionListResponse getMySections(String studentNumber, StatusType status, Integer year,
                                               SemesterType semester) {
        return SectionListResponse.from(
            sectionQueryService.getSectionsByStudentNumber(studentNumber, status, year, semester));
    }
}
