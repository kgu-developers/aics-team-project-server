package kgu.developers.api.preSurveyResponse.application;

import kgu.developers.api.preSurveyResponse.presentation.request.PreSurveyResponseSubmitRequest;
import kgu.developers.api.preSurveyResponse.presentation.response.PreSurveyResponseDetailResponse;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.preSurveyResponse.application.command.PreSurveyResponseCommandService;
import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseQueryService;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponsePreferredRolesInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PreSurveyResponseFacade {

    private final PreSurveyResponseCommandService preSurveyResponseCommandService;
    private final PreSurveyResponseQueryService preSurveyResponseQueryService;
    private final EnrollmentRepository enrollmentRepository;

    public PreSurveyResponseDetailResponse submit(Long sectionId, String userId, PreSurveyResponseSubmitRequest request) {
        validateEnrollment(sectionId, userId);
        return PreSurveyResponseDetailResponse.from(preSurveyResponseCommandService.submit(
            userId,
            sectionId,
            JsonConverter.toTree(request.preferredRoles(), PreSurveyResponsePreferredRolesInvalidException::new),
            request.topicOpinion(),
            request.etcOpinion()
        ));
    }

    public PreSurveyResponseDetailResponse getMyResponse(String userId, Long sectionId) {
        validateEnrollment(sectionId, userId);
        return PreSurveyResponseDetailResponse.from(preSurveyResponseQueryService.getResponse(userId, sectionId));
    }

    private void validateEnrollment(Long sectionId, String userId) {
        Enrollment enrollment = enrollmentRepository.findBySectionIdAndUserId(sectionId, userId)
            .orElseThrow(() -> new AccessDeniedException("해당 분반 수강생만 사전조사에 응답할 수 있습니다."));

        if (!enrollment.isActiveStudent()) {
            throw new AccessDeniedException("해당 분반 수강생만 사전조사에 응답할 수 있습니다.");
        }
    }
}
