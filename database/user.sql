-- 운영은 ddl-auto: validate이므로 api/admin/auth 배포 전에 이 DDL을 대상 DB에 먼저 적용한다.
ALTER TABLE "user"
    ADD COLUMN IF NOT EXISTS password_change_required_until TIMESTAMP(6);

COMMENT ON COLUMN "user".password_change_required_until
    IS '관리자 비밀번호 초기화 후 변경해야 하는 기한; NULL이면 요구 없음';
