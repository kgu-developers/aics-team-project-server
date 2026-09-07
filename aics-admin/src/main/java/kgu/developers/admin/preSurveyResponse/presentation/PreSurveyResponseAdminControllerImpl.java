package kgu.developers.admin.preSurveyResponse.presentation;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kgu.developers.admin.preSurveyResponse.application.PreSurveyResponseAdminFacade;
import kgu.developers.admin.preSurveyResponse.application.PreSurveyResponseExcelDownload;
import kgu.developers.admin.preSurveyResponse.presentation.response.PreSurveyResponseAdminListResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop")
public class PreSurveyResponseAdminControllerImpl implements PreSurveyResponseAdminController {

    private final PreSurveyResponseAdminFacade preSurveyResponseAdminFacade;

    @Override
    @GetMapping("/sections/{sectionId}/pre-survey-responses")
    public ResponseEntity<PreSurveyResponseAdminListResponse> getResponsesBySection(
        @PathVariable Long sectionId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            preSurveyResponseAdminFacade.getResponsesBySection(sectionId, authentication.getName()));
    }

    @Override
    @GetMapping("/sections/{sectionId}/pre-survey-responses/download")
    public ResponseEntity<byte[]> downloadResponsesExcel(
        @PathVariable Long sectionId,
        Authentication authentication
    ) {
        PreSurveyResponseExcelDownload download = preSurveyResponseAdminFacade
            .downloadResponsesExcel(sectionId, authentication.getName());
        ContentDisposition contentDisposition = ContentDisposition.attachment()
            .filename(download.fileName(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .contentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(download.content());
    }
}
