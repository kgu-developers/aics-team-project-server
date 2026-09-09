package kgu.developers.api.midreport.presentation;

import jakarta.validation.Valid;
import kgu.developers.api.midreport.application.MidReportFacade;
import kgu.developers.api.midreport.presentation.request.MidReportBlockCompletionRequest;
import kgu.developers.api.midreport.presentation.request.MidReportBlockUpdateRequest;
import kgu.developers.api.midreport.presentation.request.MidReportSubmissionRequest;
import kgu.developers.api.midreport.presentation.response.MidReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mid-reports")
@RequiredArgsConstructor
public class MidReportControllerImpl implements MidReportController {
    private final MidReportFacade midReportFacade;

    @Override
    @GetMapping("/current")
    public ResponseEntity<MidReportResponse> getCurrent(Authentication authentication) {
        return ResponseEntity.ok(midReportFacade.getCurrent(authentication.getName()));
    }

    @Override
    @PatchMapping("/{id}/blocks/{blockKey}")
    public ResponseEntity<MidReportResponse> updateBlock(
        @PathVariable Long id,
        @PathVariable String blockKey,
        @Valid @RequestBody MidReportBlockUpdateRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(midReportFacade.updateBlock(id, blockKey, authentication.getName(), request));
    }

    @Override
    @PostMapping("/{id}/blocks/{blockKey}/completion")
    public ResponseEntity<MidReportResponse> completeBlock(
        @PathVariable Long id,
        @PathVariable String blockKey,
        @Valid @RequestBody MidReportBlockCompletionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(midReportFacade.completeBlock(id, blockKey, authentication.getName(), request));
    }

    @Override
    @PostMapping("/{id}/submit")
    public ResponseEntity<MidReportResponse> submit(
        @PathVariable Long id,
        @Valid @RequestBody MidReportSubmissionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(midReportFacade.submit(id, authentication.getName(), request));
    }
}
