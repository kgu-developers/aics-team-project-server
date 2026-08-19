-- team 테이블 자체는 TeamJpaEntity 로부터 Hibernate 가 생성한다.
-- 이 파일은 엔티티로 표현할 수 없는 제약만 담는다.

-- TeamRepositoryImpl 이 이 인덱스명으로 DuplicateTeamNameException 을 판별한다.
-- 부분 인덱스인 이유: 삭제된 팀(deleted_at IS NOT NULL)은 팀명을 점유하지 않아야
-- TeamCommandService.validateNameNotTaken 의 앱 레벨 검사(삭제 팀 제외)와 결과가 같다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_team_section_name
    ON team (section_id, name)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX uk_team_section_name IS '같은 분반 안에서 팀명 중복 방지. 이름 변경 시 TeamRepositoryImpl 의 상수도 함께 수정할 것.';
