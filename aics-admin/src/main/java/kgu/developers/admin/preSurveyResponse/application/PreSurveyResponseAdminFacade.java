package kgu.developers.admin.preSurveyResponse.application;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.admin.preSurveyResponse.presentation.response.PreSurveyResponseAdminListResponse;
import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseQueryService;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;
import kgu.developers.domain.section.application.query.SectionQueryService;
import lombok.RequiredArgsConstructor;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PreSurveyResponseAdminFacade {

    private final SectionQueryService sectionQueryService;
    private final PreSurveyResponseRepository preSurveyResponseRepository;
    private final PreSurveyResponseQueryService preSurveyResponseQueryService;

    public PreSurveyResponseAdminListResponse getResponsesBySection(Long sectionId, String professorId) {
        validateSectionOwnedByProfessor(sectionId, professorId);
        return PreSurveyResponseAdminListResponse.from(preSurveyResponseRepository.findAllBySectionId(sectionId));
    }

    public PreSurveyResponseExcelDownload downloadResponsesExcel(Long sectionId, String professorId) {
        validateSectionOwnedByProfessor(sectionId, professorId);
        String sectionName = sectionQueryService.getSectionById(sectionId).section().getName();
        return new PreSurveyResponseExcelDownload(sectionName + "-사전조사.xlsx",
                preSurveyResponseQueryService.writeSectionResponsesExcel(sectionId));
    }

    private void validateSectionOwnedByProfessor(Long sectionId, String professorId) {
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
            throw new AccessDeniedException("담당 분반의 사전조사 응답만 조회할 수 있습니다.");
        }
    }
}
