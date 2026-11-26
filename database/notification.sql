-- notification 테이블 자체는 NotificationJpaEntity 로부터 Hibernate 가 생성한다.
-- 이 파일은 엔티티로 표현할 수 없는 제약과, 이미 만들어진 테이블에 대한 변경만 담는다.

-- notification.type 은 @Enumerated(STRING) 이라 Hibernate 가 테이블을 만들 때 그 시점의 enum 값으로
-- CHECK 제약(notification_type_check)을 함께 만든다. ddl-auto=update 는 이미 있는 CHECK 를 갱신하지
-- 않으므로, 공지 알림 시절(SECTION_ANNOUNCEMENT 한 개)에 만들어진 DB 에는 그 이후에 추가된
-- PRE_SURVEY_PREFERRED_PEER_* 3개가 허용되지 않은 채로 남아 있다.
-- 그 결과 NotificationOutboxWorker 가 사전조사 지목 알림을 notification 에 저장할 때 CHECK 위반으로
-- 실패하고, 아웃박스 건은 재시도 한도(3회)를 넘겨 FAILED 로 쌓였다. 지목 저장 자체는 영향이 없다.
--
-- NotificationType enum 에 값을 추가하는 PR 은 이 제약도 함께 갱신해야 한다.
-- 운영은 ddl-auto: validate 이므로 api/admin/auth 배포 전에 이 DDL 을 대상 DB 에 먼저 적용한다.
-- (DROP 과 ADD 는 한 트랜잭션으로 실행한다. 기존 행은 모두 SECTION_ANNOUNCEMENT 라 위반 행이 없다.)
BEGIN;

ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check;

ALTER TABLE notification
    ADD CONSTRAINT notification_type_check
        CHECK (type IN (
            'SECTION_ANNOUNCEMENT',
            'PRE_SURVEY_PREFERRED_PEER_REQUESTED',
            'PRE_SURVEY_PREFERRED_PEER_CANCELLED',
            'PRE_SURVEY_PREFERRED_PEER_DECIDED'
        ));

COMMIT;

-- 이미 FAILED 로 남은 notification_outbox 건(PRE_SURVEY_PREFERRED_PEER_REQUESTED/DECIDED)은
-- retry_count 가 한도에 도달해 자동 재처리되지 않는다. 알림 내용이 시점이 지났을 수 있어 재처리하지 않고
-- 그대로 둔다. 필요하면 별도로 정리한다.
