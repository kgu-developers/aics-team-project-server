package kgu.developers.api.preSurveyResponse.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.api.preSurveyResponse.application.PreSurveyResponseFacade;
import kgu.developers.api.preSurveyResponse.presentation.request.PreSurveyResponseSubmitRequest;
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
}
