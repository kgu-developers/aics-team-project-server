-- team 테이블 자체는 TeamJpaEntity 로부터 Hibernate 가 생성한다.
-- 이 파일은 엔티티로 표현할 수 없는 제약과, 이미 만들어진 테이블에 대한 변경만 담는다.

-- 낙관적 락(@Version). TeamRepositoryImpl 이 충돌을 TeamConcurrentlyModifiedException 으로 바꾼다.
-- 기존 행이 NULL 이면 Hibernate 가 신규 엔티티로 오인하므로 DEFAULT 0 으로 채운다.
ALTER TABLE team ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN team.version IS '낙관적 락. Hibernate 가 채우므로 수동 INSERT 금지.';

-- TeamRepositoryImpl 이 이 인덱스명으로 DuplicateTeamNameException 을 판별한다.
-- 부분 인덱스인 이유: 삭제된 팀(deleted_at IS NOT NULL)은 팀명을 점유하지 않아야
-- TeamCommandService.validateNameNotTaken 의 앱 레벨 검사(삭제 팀 제외)와 결과가 같다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_team_section_name
    ON team (section_id, name)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX uk_team_section_name IS '같은 분반 안에서 팀명 중복 방지. 이름 변경 시 TeamRepositoryImpl 의 상수도 함께 수정할 것.';
