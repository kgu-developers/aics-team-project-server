-- team_member 테이블 자체는 TeamMemberJpaEntity 로부터 Hibernate 가 생성한다.
-- 이 파일은 엔티티로 표현할 수 없는 제약과, 이미 만들어진 테이블에 대한 변경만 담는다.

-- 낙관적 락(@Version). TeamMemberRepositoryImpl 이 충돌을 TeamMemberConcurrentlyModifiedException 으로 바꾼다.
-- 기존 행이 NULL 이면 Hibernate 가 신규 엔티티로 오인하므로 DEFAULT 0 으로 채운다.
-- ddl-auto=update 가 먼저 version 을 nullable 로 만들어 두면 아래 ADD COLUMN 은 조용히 no-op 이 되고
-- 기존 행은 version 이 NULL 로 남는다. 그러면 Spring Data 가 그 엔티티를 신규로 판정해 UPDATE 대신
-- INSERT 를 돌리고, team_member 에서는 보통 엉뚱한 409 TEAM_MEMBER_ALREADY_EXISTS 로 드러난다.
ALTER TABLE team_member ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
UPDATE team_member SET version = 0 WHERE version IS NULL;
ALTER TABLE team_member ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE team_member ALTER COLUMN version SET NOT NULL;

COMMENT ON COLUMN team_member.version IS '낙관적 락. Hibernate 가 채우므로 수동 INSERT 금지.';

-- 엔티티의 @UniqueConstraint(전체 열 대상)에서 부분 인덱스로 교체한 것이므로, 기존 제약을 먼저 지운다.
-- 남겨두면 삭제된 팀원이 자리를 계속 점유해, 팀에서 뺐다가 같은 팀에 다시 넣을 때 항상 409 가 난다.
ALTER TABLE team_member DROP CONSTRAINT IF EXISTS uk_team_member_team_user;

-- bb84375 가 한때 엔티티에 UNIQUE(team_id, is_leader) 로 선언해 둔 적이 있다. ddl-auto=update 를 쓰는
-- dev/local DB 에는 그 제약이 남아있을 수 있는데, 그러면 한 팀에 is_leader=false 인 팀원이
-- 한 명만 존재할 수 있게 되므로 반드시 지워야 한다.
ALTER TABLE team_member DROP CONSTRAINT IF EXISTS uk_team_member_one_leader;

-- TeamMemberRepositoryImpl 이 이 인덱스명으로 TeamMemberAlreadyExistsException 을 판별한다.
-- 부분 인덱스인 이유: 삭제된 팀원(deleted_at IS NOT NULL)은 자리를 점유하지 않아야
-- TeamMemberCommandService 의 앱 레벨 검사(findByTeamIdAndUserId... AndDeletedAtIsNull)와 결과가 같다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_team_member_team_user
    ON team_member (team_id, user_id)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX uk_team_member_team_user IS '한 팀에 같은 학번 중복 방지. 이름 변경 시 TeamMemberRepositoryImpl 의 상수도 함께 수정할 것.';

-- 팀장 자진 선언의 동시성 보장. 애플리케이션의 선행 조회만으로는 동시에 도착한 요청을
-- 막을 수 없으므로, 활성 팀원 중 한 팀당 한 명만 is_leader=true 가 되도록 DB가 보장한다.
-- TeamMemberRepositoryImpl 이 이 인덱스명으로 LeaderAlreadyExistsException(409)을 판별한다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_team_member_one_leader
    ON team_member (team_id)
    WHERE is_leader = TRUE AND deleted_at IS NULL;

COMMENT ON INDEX uk_team_member_one_leader IS '활성 팀원 중 팀당 팀장 한 명 보장. 이름 변경 시 TeamMemberRepositoryImpl 의 상수도 함께 수정할 것.';
