-- team_member 테이블 자체는 TeamMemberJpaEntity 로부터 Hibernate 가 생성한다.
-- 이 파일은 엔티티로 표현할 수 없는 제약과, 이미 만들어진 테이블에 대한 변경만 담는다.

-- 낙관적 락(@Version). TeamMemberRepositoryImpl 이 충돌을 TeamMemberConcurrentlyModifiedException 으로 바꾼다.
-- 기존 행이 NULL 이면 Hibernate 가 신규 엔티티로 오인하므로 DEFAULT 0 으로 채운다.
ALTER TABLE team_member ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN team_member.version IS '낙관적 락. Hibernate 가 채우므로 수동 INSERT 금지.';

-- 엔티티의 @UniqueConstraint(전체 열 대상)에서 부분 인덱스로 교체한 것이므로, 기존 제약을 먼저 지운다.
-- 남겨두면 삭제된 팀원이 자리를 계속 점유해, 팀에서 뺐다가 같은 팀에 다시 넣을 때 항상 409 가 난다.
ALTER TABLE team_member DROP CONSTRAINT IF EXISTS uk_team_member_team_user;

-- TeamMemberRepositoryImpl 이 이 인덱스명으로 TeamMemberAlreadyExistsException 을 판별한다.
-- 부분 인덱스인 이유: 삭제된 팀원(deleted_at IS NOT NULL)은 자리를 점유하지 않아야
-- TeamMemberCommandService 의 앱 레벨 검사(findByTeamIdAndUserId... AndDeletedAtIsNull)와 결과가 같다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_team_member_team_user
    ON team_member (team_id, user_id)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX uk_team_member_team_user IS '한 팀에 같은 학번 중복 방지. 이름 변경 시 TeamMemberRepositoryImpl 의 상수도 함께 수정할 것.';
