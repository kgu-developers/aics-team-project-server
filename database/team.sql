-- team 테이블 자체는 TeamJpaEntity 로부터 Hibernate 가 생성한다.
-- 이 파일은 엔티티로 표현할 수 없는 제약과, 이미 만들어진 테이블에 대한 변경만 담는다.

-- 낙관적 락(@Version). TeamRepositoryImpl 이 충돌을 TeamConcurrentlyModifiedException 으로 바꾼다.
-- 기존 행이 NULL 이면 Hibernate 가 신규 엔티티로 오인하므로 DEFAULT 0 으로 채운다.
-- ddl-auto=update 가 먼저 version 을 nullable 로 만들어 두면 아래 ADD COLUMN 은 조용히 no-op 이 되고
-- 기존 행은 version 이 NULL 로 남는다. 그러면 Spring Data 가 그 엔티티를 신규로 판정해(@Version 이
-- 래퍼 타입일 때 isNew 기준이 id 가 아니라 version 이다) UPDATE 대신 INSERT 를 돌려 행이 복제된다.
-- 그래서 컬럼 추가만으로 끝내지 말고 NULL 을 메우고 제약을 다시 건다.
ALTER TABLE team ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
UPDATE team SET version = 0 WHERE version IS NULL;
ALTER TABLE team ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE team ALTER COLUMN version SET NOT NULL;

COMMENT ON COLUMN team.version IS '낙관적 락. Hibernate 가 채우므로 수동 INSERT 금지.';

-- bb84375 가 한때 엔티티에 @UniqueConstraint(전체 열 대상)로 선언해 둔 적이 있다. ddl-auto=update 를 쓰는
-- dev/local DB 에 그 제약이 남아있으면 삭제된 팀이 팀명을 계속 점유하므로 먼저 지운다.
ALTER TABLE team DROP CONSTRAINT IF EXISTS uk_team_section_name;

-- TeamRepositoryImpl 이 이 인덱스명으로 DuplicateTeamNameException 을 판별한다.
-- 부분 인덱스인 이유: 삭제된 팀(deleted_at IS NOT NULL)은 팀명을 점유하지 않아야
-- TeamCommandService.validateNameNotTaken 의 앱 레벨 검사(삭제 팀 제외)와 결과가 같다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_team_section_name
    ON team (section_id, name)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX uk_team_section_name IS '같은 분반 안에서 팀명 중복 방지. 이름 변경 시 TeamRepositoryImpl 의 상수도 함께 수정할 것.';
