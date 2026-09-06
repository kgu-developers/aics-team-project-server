package kgu.developers.domain.preSurveyResponse.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.exception.EnrollmentNotFoundException;
import kgu.developers.domain.notification.application.command.NotificationOutboxService;
import kgu.developers.domain.notification.domain.NotificationType;
import kgu.developers.domain.preSurveyResponse.domain.PreferredPeerStatus;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponsePreferredPeerInvalidException;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponsePreferredPeerRequestNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreSurveyResponseCommandService {
  private final PreSurveyResponseRepository preSurveyResponseRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final NotificationOutboxService notificationOutboxService;
  private final UserQueryService userQueryService;

  // 같은 사용자·분반 조합의 동시 제출은 Enrollment 행을 먼저 잠가(비관적 락) 직렬화한다 — DB
  // 유니크 제약(database/pre_survey_response.sql)은 Flyway로 자동 적용되지 않아 배포 DB에 실제로
  // 있다는 보장이 없다(sunzx0428 PR #65 리뷰 09-03). 이 잠금이 이미 요청을 직렬화하므로 유니크
  // 제약 위반을 catch해 재조회·갱신으로 복구하는 경로는 두지 않는다 — 같은 트랜잭션 안에서
  // saveAndFlush 실패 후 재조회하면 rollback-only 상태라 정상 동작을 보장할 수 없다(같은 PR
  // 09-04 리뷰).
  //
  // 지목 취소는 preferredPeerUserId 를 null 로 담아 다시 제출하는 것이다. 별도 취소 API 를 두지 않는다.
  @Transactional
  public PreSurveyResponse submit(String userId, Long sectionId, JsonNode preferredRoles,
      String topicOpinion, String etcOpinion, String preferredPeerUserId) {
    validatePreferredPeer(userId, sectionId, preferredPeerUserId);

    enrollmentRepository.findBySectionIdAndUserIdForUpdate(sectionId, userId)
        .orElseThrow(EnrollmentNotFoundException::new);

    PreSurveyResponse existing = preSurveyResponseRepository.findByUserIdAndSectionId(userId, sectionId)
        .orElse(null);

    String previousPeerUserId = existing == null ? null : existing.getPreferredPeerUserId();
    kgu.developers.domain.preSurveyResponse.domain.PreferredPeerStatus previousPeerStatus = 
        existing == null ? null : existing.getPreferredPeerStatus();

    PreSurveyResponse saved;
    if (existing != null) {
      existing.update(preferredRoles, topicOpinion, etcOpinion, preferredPeerUserId);
      saved = preSurveyResponseRepository.save(existing);
    } else {
      saved = PreSurveyResponse.create(userId, sectionId, preferredRoles, topicOpinion,
        etcOpinion, preferredPeerUserId);
      saved = preSurveyResponseRepository.save(saved);
    }

    // 트랜잭션 안에서 알림 아웃박스에 기록
    createNotificationOutboxesForPeerChange(saved, previousPeerUserId, previousPeerStatus, userId);

    return saved;
  }

  /**
   * 지목당한 학생의 수락·거절. 지목한 쪽(requester)의 응답 행을 고치므로, 그쪽이 재제출로 대상을
   * 바꾸는 것과 경합하지 않도록 requester 의 Enrollment 행을 잠근 뒤 상태를 다시 확인한다.
   */
  @Transactional
  public PreSurveyResponse decidePreferredPeer(String peerUserId, Long sectionId, String requesterUserId,
      boolean accepted) {
    enrollmentRepository.findBySectionIdAndUserIdForUpdate(sectionId, requesterUserId)
        .orElseThrow(PreSurveyResponsePreferredPeerRequestNotFoundException::new);

    PreSurveyResponse request = preSurveyResponseRepository
        .findByUserIdAndSectionId(requesterUserId, sectionId)
        .orElseThrow(PreSurveyResponsePreferredPeerRequestNotFoundException::new);

    if (!request.isPreferredPeerPendingFor(peerUserId)) {
      throw new PreSurveyResponsePreferredPeerRequestNotFoundException();
    }

    request.decidePreferredPeer(accepted);
    PreSurveyResponse saved = preSurveyResponseRepository.save(request);

    // 트랜잭션 안에서 알림 아웃박스에 기록
    String peerUserName = userQueryService.getUserByStudentNumber(peerUserId).getName();
    notificationOutboxService.createNotificationOutbox(
        requesterUserId,
        NotificationType.PRE_SURVEY_PREFERRED_PEER_DECIDED,
        saved.getId(),
        accepted ? "조원 지목 수락" : "조원 지목 거절",
        String.format("%s 님이 회원님의 조원 지목을 %s했습니다.", peerUserName, accepted ? "수락" : "거절"),
        null
    );

    return saved;
  }

  private void validatePreferredPeer(String userId, Long sectionId, String preferredPeerUserId) {
    if (preferredPeerUserId == null) {
      return;
    }
    if (preferredPeerUserId.equals(userId)) {
      throw new PreSurveyResponsePreferredPeerInvalidException();
    }
    Enrollment peer = enrollmentRepository.findBySectionIdAndUserId(sectionId, preferredPeerUserId)
        .orElseThrow(PreSurveyResponsePreferredPeerInvalidException::new);
    if (!peer.isActiveStudent()) {
      throw new PreSurveyResponsePreferredPeerInvalidException();
    }
  }

  /**
   * 지목 대상이 실제로 바뀐 경우에만 아웃박스에 기록한다. 대상 변경은 이전 대상에게는 취소, 새 대상에게는 요청이라
   * 양쪽 모두 기록한다. 대상이 그대로면(의견만 고친 재제출) 아무것도 기록하지 않는다.
   */
  private void createNotificationOutboxesForPeerChange(PreSurveyResponse saved, String previousPeerUserId, PreferredPeerStatus previousPeerStatus, String userId) {
    String peerUserId = saved.getPreferredPeerUserId();
    if (java.util.Objects.equals(previousPeerUserId, peerUserId)) {
      return;
    }

    String userName = userQueryService.getUserByStudentNumber(userId).getName();

    // 이미 거절한 상대에게 "그 지목이 취소됐다"고 보내지 않는다.
    boolean previouslyRejected = previousPeerStatus == PreferredPeerStatus.REJECTED;
    if (previousPeerUserId != null && !previouslyRejected) {
      notificationOutboxService.createNotificationOutbox(
          previousPeerUserId,
          NotificationType.PRE_SURVEY_PREFERRED_PEER_CANCELLED,
          saved.getId(),
          "조원 지목 취소",
          String.format("%s 님이 회원님에 대한 조원 지목을 취소했습니다.", userName),
          null
      );
    }

    if (peerUserId != null) {
      notificationOutboxService.createNotificationOutbox(
          peerUserId,
          NotificationType.PRE_SURVEY_PREFERRED_PEER_REQUESTED,
          saved.getId(),
          "조원 지목 요청",
          String.format("%s 님이 회원님을 조원으로 지목했습니다.", userName),
          null
      );
    }
  }
}
