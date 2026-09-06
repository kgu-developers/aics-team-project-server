package kgu.developers.api.preSurveyResponse.application;

import java.util.List;
import java.util.Locale;

import kgu.developers.api.preSurveyResponse.presentation.request.PreSurveyResponseSubmitRequest;
import kgu.developers.api.preSurveyResponse.presentation.response.PreSurveyClassmateListResponse;
import kgu.developers.api.preSurveyResponse.presentation.response.PreSurveyPreferredPeerRequestListResponse;
import kgu.developers.api.preSurveyResponse.presentation.response.PreSurveyResponseDetailResponse;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.enrollment.application.query.EnrollmentQueryService;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentDetail;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.preSurveyResponse.application.command.PreSurveyResponseCommandService;
import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseQueryService;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponsePreferredRolesInvalidException;
import kgu.developers.domain.user.application.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PreSurveyResponseFacade {

    private final PreSurveyResponseCommandService preSurveyResponseCommandService;
    private final PreSurveyResponseQueryService preSurveyResponseQueryService;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentQueryService enrollmentQueryService;
    private final UserQueryService userQueryService;

    public PreSurveyResponseDetailResponse submit(Long sectionId, String userId, PreSurveyResponseSubmitRequest request) {
        validateEnrollment(sectionId, userId);
        return PreSurveyResponseDetailResponse.from(preSurveyResponseCommandService.submit(
            userId,
            sectionId,
            JsonConverter.toTree(request.preferredRoles(), PreSurveyResponsePreferredRolesInvalidException::new),
            request.topicOpinion(),
            request.etcOpinion(),
            request.preferredPeerUserId()
        ), userName(userId));
    }

    public PreSurveyResponseDetailResponse getMyResponse(String userId, Long sectionId) {
        validateEnrollment(sectionId, userId);
        return PreSurveyResponseDetailResponse.from(
            preSurveyResponseQueryService.getResponse(userId, sectionId), userName(userId));
    }

    // ponytail: 분반 하나가 수십 명 규모라 전체 명단을 받아 메모리에서 거른다. 수백 명을 넘기면 리포지토리 검색 쿼리로 내린다.
    public PreSurveyClassmateListResponse searchClassmates(String userId, Long sectionId, String keyword) {
        validateEnrollment(sectionId, userId);

        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<EnrollmentDetail> matched = enrollmentQueryService.getEnrollmentsBySectionId(sectionId).stream()
            .filter(detail -> detail.enrollment().isActiveStudent())
            .filter(detail -> !detail.user().getStudentNumber().equals(userId))
            .filter(detail -> normalized.isEmpty()
                || detail.user().getName().toLowerCase(Locale.ROOT).contains(normalized)
                || detail.user().getStudentNumber().toLowerCase(Locale.ROOT).contains(normalized))
            .toList();

        return PreSurveyClassmateListResponse.from(matched);
    }

    public PreSurveyPreferredPeerRequestListResponse getReceivedPreferredPeerRequests(String userId, Long sectionId) {
        validateEnrollment(sectionId, userId);

        List<PreSurveyResponse> requests = preSurveyResponseQueryService
            .getReceivedPreferredPeerRequests(userId, sectionId);
        List<String> requesterIds = requests.stream().map(PreSurveyResponse::getUserId).toList();

        return PreSurveyPreferredPeerRequestListResponse.from(
            requests, userQueryService.getUsersByStudentNumbers(requesterIds));
    }

    public PreSurveyPreferredPeerRequestListResponse decidePreferredPeer(String userId, Long sectionId,
        String requesterUserId, boolean accepted) {
        validateEnrollment(sectionId, userId);
        preSurveyResponseCommandService.decidePreferredPeer(userId, sectionId, requesterUserId, accepted);
        return getReceivedPreferredPeerRequests(userId, sectionId);
    }

    private String userName(String userId) {
        return userQueryService.getUserByStudentNumber(userId).getName();
    }

    private void validateEnrollment(Long sectionId, String userId) {
        Enrollment enrollment = enrollmentRepository.findBySectionIdAndUserId(sectionId, userId)
            .orElseThrow(() -> new AccessDeniedException("해당 분반 수강생만 사전조사에 응답할 수 있습니다."));

        if (!enrollment.isActiveStudent()) {
            throw new AccessDeniedException("해당 분반 수강생만 사전조사에 응답할 수 있습니다.");
        }
    }
}
