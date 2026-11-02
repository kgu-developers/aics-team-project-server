-- 운영은 ddl-auto: validate이므로 api/admin/auth 배포 전에 이 DDL을 대상 DB에 먼저 적용한다.
-- 컬럼이 없으면 추가하고 기존 행은 0으로 시작한다.
-- 이미 존재하면 NULL만 0으로 보정하고, NULL이 아닌 버전 값은 유지한다.
ALTER TABLE proposal_section ADD COLUMN IF NOT EXISTS content_version BIGINT NOT NULL DEFAULT 0;
UPDATE proposal_section SET content_version = 0 WHERE content_version IS NULL;
ALTER TABLE proposal_section ALTER COLUMN content_version SET DEFAULT 0;
ALTER TABLE proposal_section ALTER COLUMN content_version SET NOT NULL;
