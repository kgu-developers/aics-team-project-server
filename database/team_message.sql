-- team_message 테이블 자체는 TeamMessageJpaEntity 로부터 Hibernate 가 생성한다.
-- 이 파일은 엔티티로 표현할 수 없는 제약과, 이미 만들어진 테이블에 대한 변경만 담는다.

-- 운영은 ddl-auto: validate이므로 api/admin 배포 전에 이 DDL을 대상 DB에 먼저 적용한다.
-- KD3-318(전체 팀 공지 묶음 식별자)에서 추가한 nullable 컬럼 — 분반 전체 공지처럼 한 번에
-- 여러 팀에 보낸 메시지들을 같은 broadcastId로 묶어, 관리자 쪽지함에서 한 줄로 모아 보여주고
-- 교수가 하나를 읽으면 그룹 전체가 읽힌 것으로 동기화하는 데 쓴다. 기존 1:1 메시지는 null로 둔다.
ALTER TABLE team_message ADD COLUMN IF NOT EXISTS broadcast_id VARCHAR(36);
