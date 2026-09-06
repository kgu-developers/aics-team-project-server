package kgu.developers.api.preSurveyResponse.application;

import java.util.List;
import java.util.Objects;
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
import kgu.developers.domain.notification.application.command.NotificationCommandService;
import kgu.developers.domain.notification.domain.NotificationType;
import kgu.developers.domain.preSurveyResponse.application.command.PreSurveyResponseCommandService;
import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseQueryService;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreferredPeerStatus;
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
    private final NotificationCommandService notificationCommandService;

    public PreSurveyResponseDetailResponse submit(Long sectionId, String userId, PreSurveyResponseSubmitRequest request) {
        validateEnrollment(sectionId, userId);

        // 지목 알림은 "대상이 새로 생기거나 바뀐" 경우에만 보낸다. 같은 학생을 지목한 채 의견만 고쳐
        // 재제출할 때마다 알림이 가면 스팸이 되므로, 제출 전 상태를 읽어 비교한다.
        PreSurveyResponse previous = preSurveyResponseQueryService.findResponse(userId, sectionId).orElse(null);
        String previousPeerUserId = previous == null ? null : previous.getPreferredPeerUserId();
        PreferredPeerStatus previousPeerStatus = previous == null ? null : previous.getPreferredPeerStatus();

        PreSurveyResponse saved = preSurveyResponseCommandService.submit(
            userId,
            sectionId,
            JsonConverter.toTree(request.preferredRoles(), PreSurveyResponsePreferredRolesInvalidException::new),
            request.topicOpinion(),
            request.etcOpinion(),
            request.preferredPeerUserId()
        );

        notifyPreferredPeerChanged(saved, previousPeerUserId, previousPeerStatus, userId);
        return PreSurveyResponseDetailResponse.from(saved, userName(userId));
    }

    public PreSurveyResponseDetailResponse getMyResponse(String userId, Long sectionId) {
        validateEnrollment(sectionId, userId);
        return PreSurveyResponseDetailResponse.from(
            preSurveyResponseQueryService.getResponse(userId, sectionId), userName(userId));
    }

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

        PreSurveyResponse decided = preSurveyResponseCommandService
            .decidePreferredPeer(userId, sectionId, requesterUserId, accepted);

        // 알림 저장이 실패해도 수락·거절 자체는 이미 커밋됐다. 알림 때문에 결정을 되돌리지는 않는다.
        notificationCommandService.createNotification(
            requesterUserId,
            NotificationType.PRE_SURVEY_PREFERRED_PEER_DECIDED,
            decided.getId(),
            accepted ? "조원 지목 수락" : "조원 지목 거절",
            "%s 님이 회원님의 조원 지목을 %s했습니다.".formatted(userName(userId), accepted ? "수락" : "거절"),
            null
        );

        return getReceivedPreferredPeerRequests(userId, sectionId);
    }

    /**
     * 지목 대상이 실제로 바뀐 경우에만 알린다. 대상 변경은 이전 대상에게는 취소, 새 대상에게는 요청이라
     * 양쪽 모두 발송한다. 대상이 그대로면(의견만 고친 재제출) 아무것도 보내지 않는다.
     */
    private void notifyPreferredPeerChanged(PreSurveyResponse saved, String previousPeerUserId,
        PreferredPeerStatus previousPeerStatus, String userId) {
        String peerUserId = saved.getPreferredPeerUserId();
        if (Objects.equals(previousPeerUserId, peerUserId)) {
            return;
        }

        // 이미 거절한 상대에게 "그 지목이 취소됐다"고 보내지 않는다.
        boolean previouslyRejected = previousPeerStatus == PreferredPeerStatus.REJECTED;
        if (previousPeerUserId != null && !previouslyRejected) {
            notificationCommandService.createNotification(
                previousPeerUserId,
                NotificationType.PRE_SURVEY_PREFERRED_PEER_CANCELLED,
                saved.getId(),
                "조원 지목 취소",
                "%s 님이 회원님에 대한 조원 지목을 취소했습니다.".formatted(userName(userId)),
                null
            );
        }

        if (peerUserId != null) {
            notificationCommandService.createNotification(
                peerUserId,
                NotificationType.PRE_SURVEY_PREFERRED_PEER_REQUESTED,
                saved.getId(),
                "조원 지목 요청",
                "%s 님이 회원님을 조원으로 지목했습니다.".formatted(userName(userId)),
                null
            );
        }
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
