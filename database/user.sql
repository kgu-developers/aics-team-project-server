-- 운영은 ddl-auto: validate이므로 api/admin/auth 배포 전에 이 DDL을 대상 DB에 먼저 적용한다.
ALTER TABLE "user"
    ADD COLUMN IF NOT EXISTS password_change_required BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN "user".password_change_required
    IS '관리자 비밀번호 초기화 후 비밀번호 변경 필요 여부';

-- 기존 password_change_required_until 기반 대상자의 상태를 새 컬럼으로 승계한다.
-- 레거시 컬럼이 없는 신규 환경에서는 승계 대상이 없으므로 건너뛴다.
DO $$
BEGIN
    IF EXISTS (SELECT 1
               FROM information_schema.columns
               WHERE table_name = 'user'
                 AND column_name = 'password_change_required_until') THEN
        UPDATE "user"
        SET password_change_required = TRUE
        WHERE password_change_required_until IS NOT NULL;
    END IF;
END $$;
