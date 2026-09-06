-- topic_candidate 테이블 자체는 TopicCandidateJpaEntity 로부터 Hibernate 가 생성한다.
-- 이 파일은 엔티티로 표현할 수 없는 제약만 담는다.

-- 엔티티의 @UniqueConstraint(전체 행 대상)에서 부분 인덱스로 교체한 것이므로, 기존 제약을 먼저 지운다.
-- 남겨두면 소프트 삭제된 후보가 자리를 계속 점유한다. 제안자는 후보를 지운 뒤 다른 제목으로 다시
-- 등록할 수 없고(항상 409), 남이 지운 제목도 영원히 쓸 수 없다.
ALTER TABLE topic_candidate DROP CONSTRAINT IF EXISTS uk_topic_candidate_team_proposer;
ALTER TABLE topic_candidate DROP CONSTRAINT IF EXISTS uk_topic_candidate_team_title;

-- 한 팀에서 제안자당 활성 후보 하나. 부분 인덱스인 이유는 소프트 삭제된 후보(deleted_at IS NOT NULL)가
-- 자리를 점유하지 않아야 TopicCandidateCommandService 의 앱 레벨 검사
-- (existsByTeamIdAndProposerUserId... AndDeletedAtIsNull)와 결과가 같기 때문이다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_topic_candidate_team_proposer
    ON topic_candidate (team_id, proposer_user_id)
    WHERE deleted_at IS NULL;

-- 한 팀 안에서 활성 후보 제목 중복 방지. 위와 같은 이유로 부분 인덱스다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_topic_candidate_team_title
    ON topic_candidate (team_id, title)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX uk_topic_candidate_team_proposer IS '팀당 제안자 1인 1후보(활성 행만).';
COMMENT ON INDEX uk_topic_candidate_team_title IS '팀 안에서 후보 제목 중복 방지(활성 행만).';
