package kgu.developers.admin.midreport.presentation;

import kgu.developers.admin.midreport.application.MidReportAdminFacade;
import kgu.developers.admin.midreport.presentation.request.MidReportFeedbackAdminRequest;
import kgu.developers.admin.midreport.presentation.response.MidReportAdminResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportFeedbackAdminPageResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportFeedbackAdminResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sections/{sectionId}/teams/{teamId}/mid-report")
public class MidReportAdminControllerImpl implements MidReportAdminController {

    private final MidReportAdminFacade midReportAdminFacade;

    @Override
    @GetMapping
    public ResponseEntity<MidReportAdminResponse> getMidReport(
        @PathVariable Long sectionId,
        @PathVariable Long teamId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            midReportAdminFacade.getMidReport(sectionId, teamId, authentication.getName())
        );
    }

    @Override
    @PostMapping("/feedback")
    public ResponseEntity<MidReportFeedbackAdminResponse> postFeedback(
        @PathVariable Long sectionId,
        @PathVariable Long teamId,
        @RequestBody MidReportFeedbackAdminRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            midReportAdminFacade.postFeedback(sectionId, teamId, request, authentication.getName())
        );
    }

    @Override
    @GetMapping("/feedbacks")
    public ResponseEntity<MidReportFeedbackAdminPageResponse> getFeedbacks(
        @PathVariable Long sectionId,
        @PathVariable Long teamId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            midReportAdminFacade.getFeedbacks(sectionId, teamId, PageRequest.of(page, size), authentication.getName())
        );
    }
}
