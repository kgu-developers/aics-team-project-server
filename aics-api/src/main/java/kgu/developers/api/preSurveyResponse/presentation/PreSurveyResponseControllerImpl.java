package kgu.developers.api.preSurveyResponse.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import kgu.developers.api.preSurveyResponse.application.PreSurveyResponseFacade;
import kgu.developers.api.preSurveyResponse.presentation.request.PreSurveyResponseSubmitRequest;
import kgu.developers.api.preSurveyResponse.presentation.response.PreSurveyClassmateListResponse;
import kgu.developers.api.preSurveyResponse.presentation.response.PreSurveyPreferredPeerRequestListResponse;
import kgu.developers.api.preSurveyResponse.presentation.response.PreSurveyResponseDetailResponse;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/oop")
public class PreSurveyResponseControllerImpl implements PreSurveyResponseController {

    private final PreSurveyResponseFacade preSurveyResponseFacade;

    @Override
    @PostMapping("/sections/{sectionId}/pre-survey/responses")
    public ResponseEntity<PreSurveyResponseDetailResponse> submit(
        @Positive @PathVariable Long sectionId,
        @Valid @RequestBody PreSurveyResponseSubmitRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(preSurveyResponseFacade.submit(sectionId, authentication.getName(), request));
    }

    @Override
    @GetMapping("users/me/pre-survey-response")
    public ResponseEntity<PreSurveyResponseDetailResponse> getMyResponse(
        @Positive @RequestParam Long sectionId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(preSurveyResponseFacade.getMyResponse(authentication.getName(), sectionId));
    }

    @Override
    @GetMapping("/sections/{sectionId}/pre-survey/classmates")
    public ResponseEntity<PreSurveyClassmateListResponse> searchClassmates(
        @Positive @PathVariable Long sectionId,
        @RequestParam(required = false) String keyword,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            preSurveyResponseFacade.searchClassmates(authentication.getName(), sectionId, keyword));
    }

    @Override
    @GetMapping("/sections/{sectionId}/pre-survey/preferred-peer-requests/received")
    public ResponseEntity<PreSurveyPreferredPeerRequestListResponse> getReceivedPreferredPeerRequests(
        @Positive @PathVariable Long sectionId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            preSurveyResponseFacade.getReceivedPreferredPeerRequests(authentication.getName(), sectionId));
    }

    @Override
    @PostMapping("/sections/{sectionId}/pre-survey/preferred-peer-requests/received/{requesterUserId}/accept")
    public ResponseEntity<PreSurveyPreferredPeerRequestListResponse> acceptPreferredPeerRequest(
        @Positive @PathVariable Long sectionId,
        @NotBlank @PathVariable String requesterUserId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(preSurveyResponseFacade.decidePreferredPeer(
            authentication.getName(), sectionId, requesterUserId, true));
    }

    @Override
    @PostMapping("/sections/{sectionId}/pre-survey/preferred-peer-requests/received/{requesterUserId}/reject")
    public ResponseEntity<PreSurveyPreferredPeerRequestListResponse> rejectPreferredPeerRequest(
        @Positive @PathVariable Long sectionId,
        @NotBlank @PathVariable String requesterUserId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(preSurveyResponseFacade.decidePreferredPeer(
            authentication.getName(), sectionId, requesterUserId, false));
    }
}
