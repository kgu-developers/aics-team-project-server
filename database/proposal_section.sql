-- 운영은 ddl-auto: validate이므로 api/admin/auth 배포 전에 이 DDL을 대상 DB에 먼저 적용한다.
-- 기존 섹션의 본문 버전은 0으로 시작하며, 이미 존재하는 버전 값은 유지한다.
ALTER TABLE proposal_section ADD COLUMN IF NOT EXISTS content_version BIGINT NOT NULL DEFAULT 0;
UPDATE proposal_section SET content_version = 0 WHERE content_version IS NULL;
ALTER TABLE proposal_section ALTER COLUMN content_version SET DEFAULT 0;
ALTER TABLE proposal_section ALTER COLUMN content_version SET NOT NULL;
