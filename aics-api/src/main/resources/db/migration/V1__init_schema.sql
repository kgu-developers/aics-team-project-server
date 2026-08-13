-- =====================================================================
-- CSHOME (수업 팀프로젝트 운영 플랫폼) — 통합 스키마
-- 대상 DBMS: PostgreSQL (aics-team-project-server/build.gradle 확인, org.postgresql:postgresql)
-- 근거: ERD/ERD 수정본.md(2026-07-09 확정) + ERD/OOP.png(기존 ERDCloud 실물) + PRD/05-PRD 전체 재설계.xlsx A~F 파트
--
-- 2026-07-27 개정: 기존 ERDCloud(OOP.png) 컨벤션과 대조 후 재작성.
--   - TIMESTAMP → TIMESTAMPTZ 로 전부 수정 (기존 다이어그램 실제 타입)
--   - VARCHAR 길이를 기존 다이어그램에서 확인된 값으로 정정 (email VARCHAR(100), user.name VARCHAR(50) 등)
--   - 모든 테이블/컬럼에 COMMENT 추가 — 기존 다이어그램처럼 "한글 라벨 ↔ 영문 컬럼명"이 같이 보이도록,
--     ERDCloud가 COMMENT를 논리명(한글명)으로 인식해서 가져오는 것을 전제로 함. 만약 인식 안 되면
--     이 파일의 COMMENT 문구를 그대로 각 컬럼 논리명 칸에 수동으로 옮기면 됨(찾기용 텍스트는 동일).
--   - "user"의 PK만 student_number VARCHAR(20), 나머지는 전부 BIGSERIAL id — 기존과 동일
--   - ENUM은 VARCHAR + 주석(기존 다이어그램의 "한글라벨 (VALUE1, VALUE2..)" 표시는 ERDCloud 타입 칸에
--     직접 입력된 자유 텍스트로 보여서(실제 PG ENUM 타입이 아님, 같은 라벨인 "상태"가 테이블마다 다른
--     값셋을 가짐) SQL로는 재현 불가 — import 후 타입 칸만 수동으로 다듬는 걸 권장
--   - 삭제 확정(2026-07-26): rubric, rubric_item, rubric_score — 이 스크립트에 없음, 기존 캔버스에 있으면 수동 삭제
-- =====================================================================


-- ---------------------------------------------------------------------
-- A. 아이덴티티 · 팀 라이프사이클
-- ---------------------------------------------------------------------

CREATE TABLE "user" (
    student_number VARCHAR(20) PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    name VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    global_role VARCHAR(20) NOT NULL, -- ENUM: ADMIN|PROFESSOR|STUDENT
    phone VARCHAR(20),
    must_change_password BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_user_email UNIQUE (email)
);
COMMENT ON TABLE "user" IS '회원';
COMMENT ON COLUMN "user".student_number IS '학번';
COMMENT ON COLUMN "user".email IS '이메일';
COMMENT ON COLUMN "user".name IS '이름';
COMMENT ON COLUMN "user".password IS '비밀번호';
COMMENT ON COLUMN "user".global_role IS '권한';
COMMENT ON COLUMN "user".phone IS '전화번호';
COMMENT ON COLUMN "user".must_change_password IS '비밀번호 변경 필요';
COMMENT ON COLUMN "user".created_at IS '생성일';
COMMENT ON COLUMN "user".updated_at IS '수정일';
COMMENT ON COLUMN "user".deleted_at IS '삭제일';


CREATE TABLE "course" (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    year INT NOT NULL,
    semester VARCHAR(10) NOT NULL, -- ENUM: SPRING|SUMMER|FALL|WINTER
    status VARCHAR(20) NOT NULL, -- ENUM: draft|active|archived
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
COMMENT ON TABLE "course" IS '강좌';
COMMENT ON COLUMN "course".id IS '식별자';
COMMENT ON COLUMN "course".name IS '과목명';
COMMENT ON COLUMN "course".year IS '학년도';
COMMENT ON COLUMN "course".semester IS '학기(spring,summer,fall,winter)';
COMMENT ON COLUMN "course".status IS '상태(draft,active,archived)';
COMMENT ON COLUMN "course".created_at IS '생성일';
COMMENT ON COLUMN "course".updated_at IS '수정일';
COMMENT ON COLUMN "course".deleted_at IS '삭제일';


CREATE TABLE "section" (
    id BIGSERIAL PRIMARY KEY,
    professor_id VARCHAR(20) NOT NULL,
    course_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    class_time VARCHAR(100),
    capacity INT,
    contact_visible_from TIMESTAMPTZ,
    contact_visible_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_section_course_code UNIQUE (course_id, code),
    CONSTRAINT fk_section_professor FOREIGN KEY (professor_id) REFERENCES "user"(student_number),
    CONSTRAINT fk_section_course FOREIGN KEY (course_id) REFERENCES "course"(id)
);
COMMENT ON TABLE "section" IS '분반';
COMMENT ON COLUMN "section".id IS '식별자';
COMMENT ON COLUMN "section".professor_id IS '담당교수 학번';
COMMENT ON COLUMN "section".course_id IS '강좌 식별자';
COMMENT ON COLUMN "section".code IS '과목코드';
COMMENT ON COLUMN "section".name IS '분반명';
COMMENT ON COLUMN "section".class_time IS '수업시간';
COMMENT ON COLUMN "section".capacity IS '정원';
COMMENT ON COLUMN "section".contact_visible_from IS '연락처 공개 시작(NULL=제한없음)';
COMMENT ON COLUMN "section".contact_visible_until IS '연락처 공개 종료(NULL=제한없음)';
COMMENT ON COLUMN "section".created_at IS '생성일';
COMMENT ON COLUMN "section".updated_at IS '수정일';
COMMENT ON COLUMN "section".deleted_at IS '삭제일';


CREATE TABLE "team" (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    kickoff_rule TEXT,
    meeting_schedule TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'FORMING', -- ENUM: FORMING|CONFIRMED
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_team_section FOREIGN KEY (section_id) REFERENCES "section"(id)
);
COMMENT ON TABLE "team" IS '팀';
COMMENT ON COLUMN "team".id IS '식별자';
COMMENT ON COLUMN "team".section_id IS '분반 식별자';
COMMENT ON COLUMN "team".name IS '팀명';
COMMENT ON COLUMN "team".kickoff_rule IS '팀 운영규칙';
COMMENT ON COLUMN "team".meeting_schedule IS '정기 회의일정';
COMMENT ON COLUMN "team".status IS '상태(FORMING,CONFIRMED)';
COMMENT ON COLUMN "team".created_at IS '생성일';
COMMENT ON COLUMN "team".updated_at IS '수정일';
COMMENT ON COLUMN "team".deleted_at IS '삭제일';


CREATE TABLE "enrollment" (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(20) NOT NULL,
    section_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL, -- ENUM: STUDENT|ASSISTANT
    status VARCHAR(20) NOT NULL, -- ENUM: ACTIVE|WITHDRAWN
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_enrollment_section_user UNIQUE (section_id, user_id),
    CONSTRAINT fk_enrollment_user FOREIGN KEY (user_id) REFERENCES "user"(student_number),
    CONSTRAINT fk_enrollment_section FOREIGN KEY (section_id) REFERENCES "section"(id)
);
COMMENT ON TABLE "enrollment" IS '수강/조교 등록';
COMMENT ON COLUMN "enrollment".id IS '식별자';
COMMENT ON COLUMN "enrollment".user_id IS '학번';
COMMENT ON COLUMN "enrollment".section_id IS '분반 식별자';
COMMENT ON COLUMN "enrollment".role IS '역할(STUDENT,ASSISTANT)';
COMMENT ON COLUMN "enrollment".status IS '상태(ACTIVE,WITHDRAWN)';
COMMENT ON COLUMN "enrollment".created_at IS '생성일';
COMMENT ON COLUMN "enrollment".updated_at IS '수정일';
COMMENT ON COLUMN "enrollment".deleted_at IS '삭제일';


CREATE TABLE "milestone" (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL, -- ENUM: draft|published|closed
    week_number INT,
    due_at TIMESTAMPTZ NOT NULL,
    late_submission_until TIMESTAMPTZ,
    opens_at TIMESTAMPTZ,
    evaluation_opens_at TIMESTAMPTZ,
    evaluation_closes_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_milestone_section FOREIGN KEY (section_id) REFERENCES "section"(id)
);
COMMENT ON TABLE "milestone" IS '마일스톤';
COMMENT ON COLUMN "milestone".id IS '식별자';
COMMENT ON COLUMN "milestone".section_id IS '분반 식별자';
COMMENT ON COLUMN "milestone".title IS '제목';
COMMENT ON COLUMN "milestone".description IS '설명';
COMMENT ON COLUMN "milestone".status IS '상태(draft,published,closed)';
COMMENT ON COLUMN "milestone".week_number IS '주차(순서 겸용)';
COMMENT ON COLUMN "milestone".due_at IS '마감일';
COMMENT ON COLUMN "milestone".late_submission_until IS '지각 제출 마감(NULL=불가)';
COMMENT ON COLUMN "milestone".opens_at IS '작성 가능 시작(NULL=제한없음)';
COMMENT ON COLUMN "milestone".evaluation_opens_at IS '평가 가능 시작(발표 전용, NULL=비활성)';
COMMENT ON COLUMN "milestone".evaluation_closes_at IS '평가 가능 종료(발표 전용, NULL=비활성)';
COMMENT ON COLUMN "milestone".created_at IS '생성일';
COMMENT ON COLUMN "milestone".updated_at IS '수정일';
COMMENT ON COLUMN "milestone".deleted_at IS '삭제일';


CREATE TABLE "team_member" (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    is_leader BOOLEAN NOT NULL DEFAULT false,
    project_role VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_team_member_team_user UNIQUE (team_id, user_id),
    CONSTRAINT fk_team_member_team FOREIGN KEY (team_id) REFERENCES "team"(id),
    CONSTRAINT fk_team_member_user FOREIGN KEY (user_id) REFERENCES "user"(student_number)
);
CREATE UNIQUE INDEX uq_team_member_one_leader ON "team_member"(team_id) WHERE is_leader = true;
COMMENT ON TABLE "team_member" IS '팀원';
COMMENT ON COLUMN "team_member".id IS '식별자';
COMMENT ON COLUMN "team_member".team_id IS '팀 식별자';
COMMENT ON COLUMN "team_member".user_id IS '학번';
COMMENT ON COLUMN "team_member".is_leader IS '팀장 여부';
COMMENT ON COLUMN "team_member".project_role IS '담당 역할';
COMMENT ON COLUMN "team_member".created_at IS '생성일';
COMMENT ON COLUMN "team_member".updated_at IS '수정일';
COMMENT ON COLUMN "team_member".deleted_at IS '삭제일';


CREATE TABLE "import_batch" (
    id BIGSERIAL PRIMARY KEY,
    uploaded_by VARCHAR(20) NOT NULL,
    section_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL, -- ENUM: ENROLLMENT|TEAM
    status VARCHAR(20) NOT NULL, -- ENUM: PREVIEW|APPLIED|EXPIRED
    payload JSONB,
    summary JSONB,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_import_batch_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES "user"(student_number),
    CONSTRAINT fk_import_batch_section FOREIGN KEY (section_id) REFERENCES "section"(id)
);
COMMENT ON TABLE "import_batch" IS '엑셀 임포트 배치';
COMMENT ON COLUMN "import_batch".id IS '식별자';
COMMENT ON COLUMN "import_batch".uploaded_by IS '업로더 학번';
COMMENT ON COLUMN "import_batch".section_id IS '분반 식별자';
COMMENT ON COLUMN "import_batch".type IS '유형(수강생/팀)';
COMMENT ON COLUMN "import_batch".status IS '상태(PREVIEW,APPLIED,EXPIRED)';
COMMENT ON COLUMN "import_batch".payload IS '원본 데이터(JSON)';
COMMENT ON COLUMN "import_batch".summary IS '요약(JSON)';
COMMENT ON COLUMN "import_batch".expires_at IS '만료 시각';
COMMENT ON COLUMN "import_batch".created_at IS '생성일';
COMMENT ON COLUMN "import_batch".updated_at IS '수정일';
COMMENT ON COLUMN "import_batch".deleted_at IS '삭제일';


CREATE TABLE "pre_survey_response" (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(20) NOT NULL,
    section_id BIGINT NOT NULL,
    preferred_roles JSONB,
    topic_opinion TEXT,
    etc_opinion TEXT,
    submitted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_pre_survey_response_user FOREIGN KEY (user_id) REFERENCES "user"(student_number),
    CONSTRAINT fk_pre_survey_response_section FOREIGN KEY (section_id) REFERENCES "section"(id)
);
COMMENT ON TABLE "pre_survey_response" IS '사전조사 응답';
COMMENT ON COLUMN "pre_survey_response".id IS '식별자';
COMMENT ON COLUMN "pre_survey_response".user_id IS '학번';
COMMENT ON COLUMN "pre_survey_response".section_id IS '분반 식별자';
COMMENT ON COLUMN "pre_survey_response".preferred_roles IS '희망 역할(JSON)';
COMMENT ON COLUMN "pre_survey_response".topic_opinion IS '주제 의견';
COMMENT ON COLUMN "pre_survey_response".etc_opinion IS '기타 의견';
COMMENT ON COLUMN "pre_survey_response".submitted_at IS '제출일';
COMMENT ON COLUMN "pre_survey_response".created_at IS '생성일';
COMMENT ON COLUMN "pre_survey_response".updated_at IS '수정일';
COMMENT ON COLUMN "pre_survey_response".deleted_at IS '삭제일';


-- ---------------------------------------------------------------------
-- B2. 제안서 · 주제선정
-- ---------------------------------------------------------------------

CREATE TABLE "project" (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    goal TEXT,
    repository_url VARCHAR(255),
    external_links JSONB,
    approval_status VARCHAR(20) NOT NULL, -- ENUM: draft|pending|approved|rejected
    meeting_style VARCHAR(200),
    proposal_completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_project_team UNIQUE (team_id),
    CONSTRAINT fk_project_team FOREIGN KEY (team_id) REFERENCES "team"(id)
);
COMMENT ON TABLE "project" IS '프로젝트';
COMMENT ON COLUMN "project".id IS '식별자';
COMMENT ON COLUMN "project".team_id IS '팀 식별자';
COMMENT ON COLUMN "project".title IS '제목';
COMMENT ON COLUMN "project".description IS '설명';
COMMENT ON COLUMN "project".goal IS '목표';
COMMENT ON COLUMN "project".repository_url IS '저장소 URL';
COMMENT ON COLUMN "project".external_links IS '외부 링크(JSON)';
COMMENT ON COLUMN "project".approval_status IS '승인상태(draft,pending,approved,rejected)';
COMMENT ON COLUMN "project".meeting_style IS '회의방식';
COMMENT ON COLUMN "project".proposal_completed_at IS '제안 완료 시각(세팅되면 폼 잠금)';
COMMENT ON COLUMN "project".created_at IS '생성일';
COMMENT ON COLUMN "project".updated_at IS '수정일';
COMMENT ON COLUMN "project".deleted_at IS '삭제일';


CREATE TABLE "topic_candidate" (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    proposer_user_id VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_topic_candidate_team FOREIGN KEY (team_id) REFERENCES "team"(id),
    CONSTRAINT fk_topic_candidate_proposer FOREIGN KEY (proposer_user_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "topic_candidate" IS '주제 후보';
COMMENT ON COLUMN "topic_candidate".id IS '식별자';
COMMENT ON COLUMN "topic_candidate".team_id IS '팀 식별자';
COMMENT ON COLUMN "topic_candidate".proposer_user_id IS '제안자 학번';
COMMENT ON COLUMN "topic_candidate".title IS '제목';
COMMENT ON COLUMN "topic_candidate".description IS '설명';
COMMENT ON COLUMN "topic_candidate".created_at IS '생성일';
COMMENT ON COLUMN "topic_candidate".updated_at IS '수정일';
COMMENT ON COLUMN "topic_candidate".deleted_at IS '삭제일';


CREATE TABLE "project_approval" (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_project_approval_project FOREIGN KEY (project_id) REFERENCES "project"(id),
    CONSTRAINT fk_project_approval_user FOREIGN KEY (user_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "project_approval" IS '제안서 승인(팀원 개인 동의)';
COMMENT ON COLUMN "project_approval".id IS '식별자';
COMMENT ON COLUMN "project_approval".project_id IS '프로젝트 식별자';
COMMENT ON COLUMN "project_approval".user_id IS '학번';
COMMENT ON COLUMN "project_approval".approved_at IS '동의일';
COMMENT ON COLUMN "project_approval".created_at IS '생성일';
COMMENT ON COLUMN "project_approval".updated_at IS '수정일';
COMMENT ON COLUMN "project_approval".deleted_at IS '삭제일';


CREATE TABLE "topic_vote" (
    id BIGSERIAL PRIMARY KEY,
    candidate_id BIGINT NOT NULL,
    voter_user_id VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    -- 팀당 1인 1표(같은 팀 후보 중 하나만)는 candidate.team_id 경유가 필요해 애플리케이션 레벨에서 검증
    CONSTRAINT uq_topic_vote_candidate_voter UNIQUE (candidate_id, voter_user_id),
    CONSTRAINT fk_topic_vote_candidate FOREIGN KEY (candidate_id) REFERENCES "topic_candidate"(id),
    CONSTRAINT fk_topic_vote_voter FOREIGN KEY (voter_user_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "topic_vote" IS '주제 투표';
COMMENT ON COLUMN "topic_vote".id IS '식별자';
COMMENT ON COLUMN "topic_vote".candidate_id IS '후보 식별자';
COMMENT ON COLUMN "topic_vote".voter_user_id IS '투표자 학번';
COMMENT ON COLUMN "topic_vote".created_at IS '생성일';
COMMENT ON COLUMN "topic_vote".updated_at IS '수정일';
COMMENT ON COLUMN "topic_vote".deleted_at IS '삭제일';


-- ---------------------------------------------------------------------
-- B1. 마일스톤 · 피드백 (일부)
-- ---------------------------------------------------------------------

CREATE TABLE "required_artifact" (
    id BIGSERIAL PRIMARY KEY,
    milestone_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL, -- ENUM: file|link|text
    label VARCHAR(200) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT true,
    allowed_extensions VARCHAR(200),
    max_file_size_mb INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_required_artifact_milestone FOREIGN KEY (milestone_id) REFERENCES "milestone"(id)
);
COMMENT ON TABLE "required_artifact" IS '필수 산출물 항목';
COMMENT ON COLUMN "required_artifact".id IS '식별자';
COMMENT ON COLUMN "required_artifact".milestone_id IS '마일스톤 식별자';
COMMENT ON COLUMN "required_artifact".type IS '유형(file,link,text)';
COMMENT ON COLUMN "required_artifact".label IS '라벨';
COMMENT ON COLUMN "required_artifact".is_required IS '필수 여부';
COMMENT ON COLUMN "required_artifact".allowed_extensions IS '허용 확장자';
COMMENT ON COLUMN "required_artifact".max_file_size_mb IS '최대 용량(MB)';
COMMENT ON COLUMN "required_artifact".created_at IS '생성일';
COMMENT ON COLUMN "required_artifact".updated_at IS '수정일';
COMMENT ON COLUMN "required_artifact".deleted_at IS '삭제일';


-- ---------------------------------------------------------------------
-- B3. 제출 · 이력 · 발표 (일부) / 공통 FileObject
-- ---------------------------------------------------------------------

CREATE TABLE "file_object" (
    id BIGSERIAL PRIMARY KEY,
    uploaded_by VARCHAR(20) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    size BIGINT,
    preview_supported BOOLEAN NOT NULL DEFAULT false,
    preview_type VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_file_object_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "file_object" IS '첨부파일';
COMMENT ON COLUMN "file_object".id IS '식별자';
COMMENT ON COLUMN "file_object".uploaded_by IS '업로더 학번';
COMMENT ON COLUMN "file_object".storage_key IS '저장 경로';
COMMENT ON COLUMN "file_object".file_name IS '파일명';
COMMENT ON COLUMN "file_object".content_type IS '파일 타입';
COMMENT ON COLUMN "file_object".size IS '크기(byte)';
COMMENT ON COLUMN "file_object".preview_supported IS '미리보기 지원 여부';
COMMENT ON COLUMN "file_object".preview_type IS '미리보기 유형';
COMMENT ON COLUMN "file_object".created_at IS '생성일';
COMMENT ON COLUMN "file_object".updated_at IS '수정일';
COMMENT ON COLUMN "file_object".deleted_at IS '삭제일';


CREATE TABLE "submission" (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    milestone_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL, -- ENUM: not_submitted|submitted|feedback_provided|revision_requested|approved
    current_version INT NOT NULL DEFAULT 0,
    revision_due_at TIMESTAMPTZ,
    revision_progress VARCHAR(30), -- ENUM: not_started|in_progress|ready_to_resubmit
    reopened_at TIMESTAMPTZ,
    reopened_by VARCHAR(20),
    presentation_order INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_submission_team_milestone UNIQUE (team_id, milestone_id),
    CONSTRAINT fk_submission_team FOREIGN KEY (team_id) REFERENCES "team"(id),
    CONSTRAINT fk_submission_milestone FOREIGN KEY (milestone_id) REFERENCES "milestone"(id),
    CONSTRAINT fk_submission_reopened_by FOREIGN KEY (reopened_by) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "submission" IS '제출';
COMMENT ON COLUMN "submission".id IS '식별자';
COMMENT ON COLUMN "submission".team_id IS '팀 식별자';
COMMENT ON COLUMN "submission".milestone_id IS '마일스톤 식별자';
COMMENT ON COLUMN "submission".status IS '상태(not_submitted,submitted,feedback_provided,revision_requested,approved)';
COMMENT ON COLUMN "submission".current_version IS '현재 버전(0=미제출)';
COMMENT ON COLUMN "submission".revision_due_at IS '재제출 마감(NULL=불가)';
COMMENT ON COLUMN "submission".revision_progress IS '수정 진행 상태(not_started,in_progress,ready_to_resubmit)';
COMMENT ON COLUMN "submission".reopened_at IS '재오픈 시각';
COMMENT ON COLUMN "submission".reopened_by IS '재오픈 처리자 학번';
COMMENT ON COLUMN "submission".presentation_order IS '발표 순서(발표 마일스톤 전용)';
COMMENT ON COLUMN "submission".created_at IS '생성일';
COMMENT ON COLUMN "submission".updated_at IS '수정일';
COMMENT ON COLUMN "submission".deleted_at IS '삭제일';


CREATE TABLE "submission_version" (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    submitted_by VARCHAR(20) NOT NULL,
    version INT NOT NULL,
    description TEXT,
    change_note TEXT,
    submitted_at TIMESTAMPTZ NOT NULL,
    is_late BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_submission_version_submission_version UNIQUE (submission_id, version),
    CONSTRAINT fk_submission_version_submission FOREIGN KEY (submission_id) REFERENCES "submission"(id),
    CONSTRAINT fk_submission_version_submitted_by FOREIGN KEY (submitted_by) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "submission_version" IS '제출이력';
COMMENT ON COLUMN "submission_version".id IS '식별자';
COMMENT ON COLUMN "submission_version".submission_id IS '제출 식별자';
COMMENT ON COLUMN "submission_version".submitted_by IS '제출자 학번';
COMMENT ON COLUMN "submission_version".version IS '버전';
COMMENT ON COLUMN "submission_version".description IS '설명';
COMMENT ON COLUMN "submission_version".change_note IS '변경 메모';
COMMENT ON COLUMN "submission_version".submitted_at IS '제출일';
COMMENT ON COLUMN "submission_version".is_late IS '지각 여부';
COMMENT ON COLUMN "submission_version".created_at IS '생성일';
COMMENT ON COLUMN "submission_version".updated_at IS '수정일';
COMMENT ON COLUMN "submission_version".deleted_at IS '삭제일';


CREATE TABLE "submission_artifact" (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL,
    required_artifact_id BIGINT,
    file_id BIGINT,
    type VARCHAR(20) NOT NULL, -- ENUM: file|link|text|cheerpj_run
    url VARCHAR(500),
    content TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_submission_artifact_version FOREIGN KEY (version_id) REFERENCES "submission_version"(id),
    CONSTRAINT fk_submission_artifact_required FOREIGN KEY (required_artifact_id) REFERENCES "required_artifact"(id),
    CONSTRAINT fk_submission_artifact_file FOREIGN KEY (file_id) REFERENCES "file_object"(id)
);
COMMENT ON TABLE "submission_artifact" IS '제출산출물';
COMMENT ON COLUMN "submission_artifact".id IS '식별자';
COMMENT ON COLUMN "submission_artifact".version_id IS '제출이력 식별자';
COMMENT ON COLUMN "submission_artifact".required_artifact_id IS '필수산출물 식별자';
COMMENT ON COLUMN "submission_artifact".file_id IS '첨부파일 식별자';
COMMENT ON COLUMN "submission_artifact".type IS '유형(file,link,text,cheerpj_run)';
COMMENT ON COLUMN "submission_artifact".url IS '링크';
COMMENT ON COLUMN "submission_artifact".content IS '내용';
COMMENT ON COLUMN "submission_artifact".created_at IS '생성일';
COMMENT ON COLUMN "submission_artifact".updated_at IS '수정일';
COMMENT ON COLUMN "submission_artifact".deleted_at IS '삭제일';


CREATE TABLE "submission_member_confirmation" (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    confirmed_final_report BOOLEAN NOT NULL DEFAULT false,
    confirmed_artifacts BOOLEAN NOT NULL DEFAULT false,
    one_line_review TEXT,
    confirmed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_submission_member_confirmation UNIQUE (submission_id, user_id),
    CONSTRAINT fk_submission_member_confirmation_submission FOREIGN KEY (submission_id) REFERENCES "submission"(id),
    CONSTRAINT fk_submission_member_confirmation_user FOREIGN KEY (user_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "submission_member_confirmation" IS '팀원 최종확인';
COMMENT ON COLUMN "submission_member_confirmation".id IS '식별자';
COMMENT ON COLUMN "submission_member_confirmation".submission_id IS '제출 식별자';
COMMENT ON COLUMN "submission_member_confirmation".user_id IS '학번';
COMMENT ON COLUMN "submission_member_confirmation".confirmed_final_report IS '최종보고서 확인 여부';
COMMENT ON COLUMN "submission_member_confirmation".confirmed_artifacts IS '제출물 확인 여부';
COMMENT ON COLUMN "submission_member_confirmation".one_line_review IS '한 줄 소감';
COMMENT ON COLUMN "submission_member_confirmation".confirmed_at IS '확인일';
COMMENT ON COLUMN "submission_member_confirmation".created_at IS '생성일';
COMMENT ON COLUMN "submission_member_confirmation".updated_at IS '수정일';
COMMENT ON COLUMN "submission_member_confirmation".deleted_at IS '삭제일';


CREATE TABLE "presentation_content" (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    intro_text TEXT,
    features JSONB,
    screens JSONB,
    youtube_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_presentation_content_submission UNIQUE (submission_id),
    CONSTRAINT fk_presentation_content_submission FOREIGN KEY (submission_id) REFERENCES "submission"(id)
);
COMMENT ON TABLE "presentation_content" IS '발표 공개자료';
COMMENT ON COLUMN "presentation_content".id IS '식별자';
COMMENT ON COLUMN "presentation_content".submission_id IS '제출 식별자';
COMMENT ON COLUMN "presentation_content".intro_text IS '소개글';
COMMENT ON COLUMN "presentation_content".features IS '주요기능(JSON)';
COMMENT ON COLUMN "presentation_content".screens IS '주요화면(JSON)';
COMMENT ON COLUMN "presentation_content".youtube_url IS '시연영상 링크';
COMMENT ON COLUMN "presentation_content".created_at IS '생성일';
COMMENT ON COLUMN "presentation_content".updated_at IS '수정일';
COMMENT ON COLUMN "presentation_content".deleted_at IS '삭제일';


-- ---------------------------------------------------------------------
-- B1. 마일스톤 · 피드백 (나머지 — Submission 이후에 와야 하는 것들)
-- ---------------------------------------------------------------------

CREATE TABLE "review" (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL,
    reviewer_id VARCHAR(20) NOT NULL,
    result_status VARCHAR(30) NOT NULL, -- ENUM: feedback_provided|revision_requested|approved
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_review_version UNIQUE (version_id),
    CONSTRAINT fk_review_version FOREIGN KEY (version_id) REFERENCES "submission_version"(id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "review" IS '리뷰';
COMMENT ON COLUMN "review".id IS '식별자';
COMMENT ON COLUMN "review".version_id IS '제출이력 식별자';
COMMENT ON COLUMN "review".reviewer_id IS '검토자 학번';
COMMENT ON COLUMN "review".result_status IS '결과상태(feedback_provided,revision_requested,approved)';
COMMENT ON COLUMN "review".comment IS '코멘트';
COMMENT ON COLUMN "review".created_at IS '생성일';
COMMENT ON COLUMN "review".updated_at IS '수정일';
COMMENT ON COLUMN "review".deleted_at IS '삭제일';


CREATE TABLE "mid_report_feedback" (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    author_id VARCHAR(20) NOT NULL,
    onsite_feedback_summary TEXT NOT NULL,
    professor_additional_feedback TEXT,
    revision_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_mid_report_feedback_submission FOREIGN KEY (submission_id) REFERENCES "submission"(id),
    CONSTRAINT fk_mid_report_feedback_author FOREIGN KEY (author_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "mid_report_feedback" IS '중간보고 대면피드백';
COMMENT ON COLUMN "mid_report_feedback".id IS '식별자';
COMMENT ON COLUMN "mid_report_feedback".submission_id IS '제출 식별자';
COMMENT ON COLUMN "mid_report_feedback".author_id IS '작성자 학번';
COMMENT ON COLUMN "mid_report_feedback".onsite_feedback_summary IS '대면피드백 요약(학생 선작성)';
COMMENT ON COLUMN "mid_report_feedback".professor_additional_feedback IS '교수 추가피드백(선택)';
COMMENT ON COLUMN "mid_report_feedback".revision_note IS '수정메모';
COMMENT ON COLUMN "mid_report_feedback".created_at IS '생성일';
COMMENT ON COLUMN "mid_report_feedback".updated_at IS '수정일';
COMMENT ON COLUMN "mid_report_feedback".deleted_at IS '삭제일';


-- ---------------------------------------------------------------------
-- C. 회의록 · 액션플랜
-- ---------------------------------------------------------------------

CREATE TABLE "meeting_record" (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    milestone_id BIGINT,
    author_id VARCHAR(20) NOT NULL,
    meeting_at TIMESTAMPTZ NOT NULL,
    location VARCHAR(200),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_meeting_record_team FOREIGN KEY (team_id) REFERENCES "team"(id),
    CONSTRAINT fk_meeting_record_milestone FOREIGN KEY (milestone_id) REFERENCES "milestone"(id),
    CONSTRAINT fk_meeting_record_author FOREIGN KEY (author_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "meeting_record" IS '회의록';
COMMENT ON COLUMN "meeting_record".id IS '식별자';
COMMENT ON COLUMN "meeting_record".team_id IS '팀 식별자';
COMMENT ON COLUMN "meeting_record".milestone_id IS '마일스톤 식별자(제안/중간/최종 구분, NULL 허용)';
COMMENT ON COLUMN "meeting_record".author_id IS '작성자 학번';
COMMENT ON COLUMN "meeting_record".meeting_at IS '회의 일시';
COMMENT ON COLUMN "meeting_record".location IS '장소/진행방식';
COMMENT ON COLUMN "meeting_record".content IS '회의 내용';
COMMENT ON COLUMN "meeting_record".created_at IS '생성일';
COMMENT ON COLUMN "meeting_record".updated_at IS '수정일';
COMMENT ON COLUMN "meeting_record".deleted_at IS '삭제일';


CREATE TABLE "meeting_participant" (
    id BIGSERIAL PRIMARY KEY,
    meeting_record_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_meeting_participant UNIQUE (meeting_record_id, user_id),
    CONSTRAINT fk_meeting_participant_record FOREIGN KEY (meeting_record_id) REFERENCES "meeting_record"(id),
    CONSTRAINT fk_meeting_participant_user FOREIGN KEY (user_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "meeting_participant" IS '회의 참석자';
COMMENT ON COLUMN "meeting_participant".id IS '식별자';
COMMENT ON COLUMN "meeting_participant".meeting_record_id IS '회의록 식별자';
COMMENT ON COLUMN "meeting_participant".user_id IS '학번';
COMMENT ON COLUMN "meeting_participant".created_at IS '생성일';
COMMENT ON COLUMN "meeting_participant".updated_at IS '수정일';
COMMENT ON COLUMN "meeting_participant".deleted_at IS '삭제일';


CREATE TABLE "meeting_action" (
    id BIGSERIAL PRIMARY KEY,
    meeting_record_id BIGINT NOT NULL,
    assignee_id VARCHAR(20),
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL, -- ENUM: DONE|IN_PROGRESS|EXCLUDED (UI 라벨: 완료·진행중·제외)
    due_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_meeting_action_record FOREIGN KEY (meeting_record_id) REFERENCES "meeting_record"(id),
    CONSTRAINT fk_meeting_action_assignee FOREIGN KEY (assignee_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "meeting_action" IS '액션플랜';
COMMENT ON COLUMN "meeting_action".id IS '식별자';
COMMENT ON COLUMN "meeting_action".meeting_record_id IS '회의록 식별자';
COMMENT ON COLUMN "meeting_action".assignee_id IS '담당자 학번(NULL=미지정)';
COMMENT ON COLUMN "meeting_action".content IS '내용';
COMMENT ON COLUMN "meeting_action".status IS '상태(DONE,IN_PROGRESS,EXCLUDED)';
COMMENT ON COLUMN "meeting_action".due_at IS '마감일(선택)';
COMMENT ON COLUMN "meeting_action".created_at IS '생성일';
COMMENT ON COLUMN "meeting_action".updated_at IS '수정일';
COMMENT ON COLUMN "meeting_action".deleted_at IS '삭제일';


-- ---------------------------------------------------------------------
-- D. 팀 커뮤니케이션
-- ---------------------------------------------------------------------

CREATE TABLE "team_thread" (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_team_thread_team UNIQUE (team_id),
    CONSTRAINT fk_team_thread_team FOREIGN KEY (team_id) REFERENCES "team"(id)
);
COMMENT ON TABLE "team_thread" IS '팀 대화방';
COMMENT ON COLUMN "team_thread".id IS '식별자';
COMMENT ON COLUMN "team_thread".team_id IS '팀 식별자';
COMMENT ON COLUMN "team_thread".created_at IS '생성일';
COMMENT ON COLUMN "team_thread".updated_at IS '수정일';
COMMENT ON COLUMN "team_thread".deleted_at IS '삭제일';


CREATE TABLE "team_message" (
    id BIGSERIAL PRIMARY KEY,
    thread_id BIGINT NOT NULL,
    sender_id VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    related_type VARCHAR(30) NOT NULL, -- ENUM: PROPOSAL|MEETING|MID_REPORT|FINAL_SUBMISSION|REVIEW|QUESTION|GENERAL
    related_id BIGINT, -- 폴리모픽 참조(related_type에 따라 대상 테이블이 다름) — FK 없음, 의도적
    is_important BOOLEAN NOT NULL DEFAULT false,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_team_message_thread FOREIGN KEY (thread_id) REFERENCES "team_thread"(id),
    CONSTRAINT fk_team_message_sender FOREIGN KEY (sender_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "team_message" IS '팀 메시지';
COMMENT ON COLUMN "team_message".id IS '식별자';
COMMENT ON COLUMN "team_message".thread_id IS '대화방 식별자';
COMMENT ON COLUMN "team_message".sender_id IS '발신자 학번';
COMMENT ON COLUMN "team_message".message IS '내용';
COMMENT ON COLUMN "team_message".related_type IS '관련유형(PROPOSAL,MEETING,MID_REPORT,FINAL_SUBMISSION,REVIEW,QUESTION,GENERAL)';
COMMENT ON COLUMN "team_message".related_id IS '관련 대상 식별자(폴리모픽, FK 아님)';
COMMENT ON COLUMN "team_message".is_important IS '중요 표시';
COMMENT ON COLUMN "team_message".is_read IS '읽음 여부';
COMMENT ON COLUMN "team_message".created_at IS '생성일';
COMMENT ON COLUMN "team_message".updated_at IS '수정일';
COMMENT ON COLUMN "team_message".deleted_at IS '삭제일';


CREATE TABLE "section_announcement" (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    published_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_section_announcement_section FOREIGN KEY (section_id) REFERENCES "section"(id)
);
COMMENT ON TABLE "section_announcement" IS '분반 공지사항';
COMMENT ON COLUMN "section_announcement".id IS '식별자';
COMMENT ON COLUMN "section_announcement".section_id IS '분반 식별자';
COMMENT ON COLUMN "section_announcement".title IS '제목';
COMMENT ON COLUMN "section_announcement".content IS '내용';
COMMENT ON COLUMN "section_announcement".published_at IS '게시일';
COMMENT ON COLUMN "section_announcement".created_at IS '생성일';
COMMENT ON COLUMN "section_announcement".updated_at IS '수정일';
COMMENT ON COLUMN "section_announcement".deleted_at IS '삭제일';


-- ---------------------------------------------------------------------
-- E. 평가 · 성적
-- ---------------------------------------------------------------------

CREATE TABLE "team_evaluation_criterion" (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    max_score INT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_team_evaluation_criterion_section FOREIGN KEY (section_id) REFERENCES "section"(id)
);
COMMENT ON TABLE "team_evaluation_criterion" IS '팀평가 항목';
COMMENT ON COLUMN "team_evaluation_criterion".id IS '식별자';
COMMENT ON COLUMN "team_evaluation_criterion".section_id IS '분반 식별자';
COMMENT ON COLUMN "team_evaluation_criterion".title IS '제목';
COMMENT ON COLUMN "team_evaluation_criterion".max_score IS '배점';
COMMENT ON COLUMN "team_evaluation_criterion".display_order IS '표시순서';
COMMENT ON COLUMN "team_evaluation_criterion".created_at IS '생성일';
COMMENT ON COLUMN "team_evaluation_criterion".updated_at IS '수정일';
COMMENT ON COLUMN "team_evaluation_criterion".deleted_at IS '삭제일';


CREATE TABLE "team_evaluation" (
    id BIGSERIAL PRIMARY KEY,
    milestone_id BIGINT NOT NULL,
    rater_id VARCHAR(20) NOT NULL,
    ratee_team_id BIGINT NOT NULL,
    submitted_at TIMESTAMPTZ, -- NULL=초안, 값 있으면 최종 제출
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    -- 본인 소속 팀 평가 금지는 team_member 조인이 필요해 애플리케이션 레벨에서 검증
    CONSTRAINT uq_team_evaluation UNIQUE (milestone_id, rater_id, ratee_team_id),
    CONSTRAINT fk_team_evaluation_milestone FOREIGN KEY (milestone_id) REFERENCES "milestone"(id),
    CONSTRAINT fk_team_evaluation_rater FOREIGN KEY (rater_id) REFERENCES "user"(student_number),
    CONSTRAINT fk_team_evaluation_ratee_team FOREIGN KEY (ratee_team_id) REFERENCES "team"(id)
);
COMMENT ON TABLE "team_evaluation" IS '팀간 평가';
COMMENT ON COLUMN "team_evaluation".id IS '식별자';
COMMENT ON COLUMN "team_evaluation".milestone_id IS '마일스톤 식별자(최종 발표)';
COMMENT ON COLUMN "team_evaluation".rater_id IS '평가자 학번';
COMMENT ON COLUMN "team_evaluation".ratee_team_id IS '피평가팀 식별자';
COMMENT ON COLUMN "team_evaluation".submitted_at IS '제출일(NULL=초안)';
COMMENT ON COLUMN "team_evaluation".created_at IS '생성일';
COMMENT ON COLUMN "team_evaluation".updated_at IS '수정일';
COMMENT ON COLUMN "team_evaluation".deleted_at IS '삭제일';


CREATE TABLE "team_evaluation_score" (
    id BIGSERIAL PRIMARY KEY,
    team_evaluation_id BIGINT NOT NULL,
    criterion_id BIGINT NOT NULL,
    score INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_team_evaluation_score UNIQUE (team_evaluation_id, criterion_id),
    CONSTRAINT fk_team_evaluation_score_evaluation FOREIGN KEY (team_evaluation_id) REFERENCES "team_evaluation"(id),
    CONSTRAINT fk_team_evaluation_score_criterion FOREIGN KEY (criterion_id) REFERENCES "team_evaluation_criterion"(id)
);
COMMENT ON TABLE "team_evaluation_score" IS '팀평가 점수';
COMMENT ON COLUMN "team_evaluation_score".id IS '식별자';
COMMENT ON COLUMN "team_evaluation_score".team_evaluation_id IS '팀평가 식별자';
COMMENT ON COLUMN "team_evaluation_score".criterion_id IS '평가항목 식별자';
COMMENT ON COLUMN "team_evaluation_score".score IS '점수';
COMMENT ON COLUMN "team_evaluation_score".created_at IS '생성일';
COMMENT ON COLUMN "team_evaluation_score".updated_at IS '수정일';
COMMENT ON COLUMN "team_evaluation_score".deleted_at IS '삭제일';


CREATE TABLE "peer_evaluation_form" (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL,
    milestone_id BIGINT,
    title VARCHAR(200) NOT NULL,
    is_anonymous BOOLEAN NOT NULL DEFAULT true,
    opens_at TIMESTAMPTZ NOT NULL,
    closes_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_peer_evaluation_form_section FOREIGN KEY (section_id) REFERENCES "section"(id),
    CONSTRAINT fk_peer_evaluation_form_milestone FOREIGN KEY (milestone_id) REFERENCES "milestone"(id)
);
COMMENT ON TABLE "peer_evaluation_form" IS '상호평가 폼';
COMMENT ON COLUMN "peer_evaluation_form".id IS '식별자';
COMMENT ON COLUMN "peer_evaluation_form".section_id IS '분반 식별자';
COMMENT ON COLUMN "peer_evaluation_form".milestone_id IS '마일스톤 식별자(선택)';
COMMENT ON COLUMN "peer_evaluation_form".title IS '제목';
COMMENT ON COLUMN "peer_evaluation_form".is_anonymous IS '익명 여부';
COMMENT ON COLUMN "peer_evaluation_form".opens_at IS '시작일';
COMMENT ON COLUMN "peer_evaluation_form".closes_at IS '종료일';
COMMENT ON COLUMN "peer_evaluation_form".created_at IS '생성일';
COMMENT ON COLUMN "peer_evaluation_form".updated_at IS '수정일';
COMMENT ON COLUMN "peer_evaluation_form".deleted_at IS '삭제일';


CREATE TABLE "peer_evaluation_question" (
    id BIGSERIAL PRIMARY KEY,
    form_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    type VARCHAR(10) NOT NULL, -- ENUM: SCALE|TEXT
    max_score INT,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_peer_evaluation_question_form FOREIGN KEY (form_id) REFERENCES "peer_evaluation_form"(id)
);
COMMENT ON TABLE "peer_evaluation_question" IS '상호평가 문항';
COMMENT ON COLUMN "peer_evaluation_question".id IS '식별자';
COMMENT ON COLUMN "peer_evaluation_question".form_id IS '폼 식별자';
COMMENT ON COLUMN "peer_evaluation_question".question_text IS '문항내용';
COMMENT ON COLUMN "peer_evaluation_question".type IS '유형(SCALE,TEXT)';
COMMENT ON COLUMN "peer_evaluation_question".max_score IS '배점';
COMMENT ON COLUMN "peer_evaluation_question".display_order IS '표시순서';
COMMENT ON COLUMN "peer_evaluation_question".created_at IS '생성일';
COMMENT ON COLUMN "peer_evaluation_question".updated_at IS '수정일';
COMMENT ON COLUMN "peer_evaluation_question".deleted_at IS '삭제일';


CREATE TABLE "peer_evaluation_response" (
    id BIGSERIAL PRIMARY KEY,
    form_id BIGINT NOT NULL,
    evaluator_id VARCHAR(20) NOT NULL,
    target_id VARCHAR(20) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    self_contribution TEXT,
    project_review_comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_peer_evaluation_response UNIQUE (form_id, evaluator_id, target_id),
    CONSTRAINT fk_peer_evaluation_response_form FOREIGN KEY (form_id) REFERENCES "peer_evaluation_form"(id),
    CONSTRAINT fk_peer_evaluation_response_evaluator FOREIGN KEY (evaluator_id) REFERENCES "user"(student_number),
    CONSTRAINT fk_peer_evaluation_response_target FOREIGN KEY (target_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "peer_evaluation_response" IS '상호평가 응답';
COMMENT ON COLUMN "peer_evaluation_response".id IS '식별자';
COMMENT ON COLUMN "peer_evaluation_response".form_id IS '폼 식별자';
COMMENT ON COLUMN "peer_evaluation_response".evaluator_id IS '평가자 학번(익명여부 무관 항상 저장)';
COMMENT ON COLUMN "peer_evaluation_response".target_id IS '대상자 학번';
COMMENT ON COLUMN "peer_evaluation_response".submitted_at IS '제출일';
COMMENT ON COLUMN "peer_evaluation_response".self_contribution IS '본인 기여내용';
COMMENT ON COLUMN "peer_evaluation_response".project_review_comment IS '프로젝트 소감';
COMMENT ON COLUMN "peer_evaluation_response".created_at IS '생성일';
COMMENT ON COLUMN "peer_evaluation_response".updated_at IS '수정일';
COMMENT ON COLUMN "peer_evaluation_response".deleted_at IS '삭제일';


CREATE TABLE "peer_evaluation_answer" (
    id BIGSERIAL PRIMARY KEY,
    response_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    score INT,
    text_answer TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_peer_evaluation_answer_response FOREIGN KEY (response_id) REFERENCES "peer_evaluation_response"(id),
    CONSTRAINT fk_peer_evaluation_answer_question FOREIGN KEY (question_id) REFERENCES "peer_evaluation_question"(id)
);
COMMENT ON TABLE "peer_evaluation_answer" IS '상호평가 답변';
COMMENT ON COLUMN "peer_evaluation_answer".id IS '식별자';
COMMENT ON COLUMN "peer_evaluation_answer".response_id IS '응답 식별자';
COMMENT ON COLUMN "peer_evaluation_answer".question_id IS '문항 식별자';
COMMENT ON COLUMN "peer_evaluation_answer".score IS '점수(기여도로 쓰일 때)';
COMMENT ON COLUMN "peer_evaluation_answer".text_answer IS '서술답변(한줄평가로 쓰일 때)';
COMMENT ON COLUMN "peer_evaluation_answer".created_at IS '생성일';
COMMENT ON COLUMN "peer_evaluation_answer".updated_at IS '수정일';
COMMENT ON COLUMN "peer_evaluation_answer".deleted_at IS '삭제일';


CREATE TABLE "grade" (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    user_id VARCHAR(20),
    team_score NUMERIC(6,2),
    peer_factor NUMERIC(6,2),
    final_score NUMERIC(6,2),
    manual_adjustment NUMERIC(6,2),
    adjustment_reason VARCHAR(500),
    finalized_at TIMESTAMPTZ,
    snapshot JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_grade_section FOREIGN KEY (section_id) REFERENCES "section"(id),
    CONSTRAINT fk_grade_team FOREIGN KEY (team_id) REFERENCES "team"(id),
    CONSTRAINT fk_grade_user FOREIGN KEY (user_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "grade" IS '성적';
COMMENT ON COLUMN "grade".id IS '식별자';
COMMENT ON COLUMN "grade".section_id IS '분반 식별자';
COMMENT ON COLUMN "grade".team_id IS '팀 식별자';
COMMENT ON COLUMN "grade".user_id IS '학번';
COMMENT ON COLUMN "grade".team_score IS '팀점수';
COMMENT ON COLUMN "grade".peer_factor IS '상호평가 반영계수';
COMMENT ON COLUMN "grade".final_score IS '최종점수';
COMMENT ON COLUMN "grade".manual_adjustment IS '수동조정 점수';
COMMENT ON COLUMN "grade".adjustment_reason IS '조정사유';
COMMENT ON COLUMN "grade".finalized_at IS '확정일';
COMMENT ON COLUMN "grade".snapshot IS '확정시점 스냅샷(JSON)';
COMMENT ON COLUMN "grade".created_at IS '생성일';
COMMENT ON COLUMN "grade".updated_at IS '수정일';
COMMENT ON COLUMN "grade".deleted_at IS '삭제일';


-- ---------------------------------------------------------------------
-- 공통 (크로스커팅)
-- ---------------------------------------------------------------------

CREATE TABLE "notification" (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(20) NOT NULL,
    type VARCHAR(30) NOT NULL, -- ENUM: milestone_published|deadline_soon|submission_completed|feedback_created|revision_requested|team_changed
    title VARCHAR(200) NOT NULL,
    message TEXT,
    link VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "notification" IS '알림';
COMMENT ON COLUMN "notification".id IS '식별자';
COMMENT ON COLUMN "notification".user_id IS '학번';
COMMENT ON COLUMN "notification".type IS '유형(milestone_published 등)';
COMMENT ON COLUMN "notification".title IS '제목';
COMMENT ON COLUMN "notification".message IS '내용';
COMMENT ON COLUMN "notification".link IS '링크';
COMMENT ON COLUMN "notification".is_read IS '읽음 여부';
COMMENT ON COLUMN "notification".created_at IS '생성일';
COMMENT ON COLUMN "notification".updated_at IS '수정일';
COMMENT ON COLUMN "notification".deleted_at IS '삭제일';


CREATE TABLE "audit_log" (
    id BIGSERIAL PRIMARY KEY,
    actor_id VARCHAR(20) NOT NULL,
    section_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_id) REFERENCES "user"(student_number),
    CONSTRAINT fk_audit_log_section FOREIGN KEY (section_id) REFERENCES "section"(id)
);
COMMENT ON TABLE "audit_log" IS '감사로그';
COMMENT ON COLUMN "audit_log".id IS '식별자';
COMMENT ON COLUMN "audit_log".actor_id IS '행위자 학번';
COMMENT ON COLUMN "audit_log".section_id IS '분반 식별자(의도적 비정규화, 조교 필터용)';
COMMENT ON COLUMN "audit_log".event_type IS '이벤트 유형';
COMMENT ON COLUMN "audit_log".target_type IS '대상 유형(폴리모픽, FK 아님)';
COMMENT ON COLUMN "audit_log".target_id IS '대상 식별자(폴리모픽, FK 아님)';
COMMENT ON COLUMN "audit_log".metadata IS '부가정보(JSON)';
COMMENT ON COLUMN "audit_log".created_at IS '생성일';
COMMENT ON COLUMN "audit_log".updated_at IS '수정일';
COMMENT ON COLUMN "audit_log".deleted_at IS '삭제일';


CREATE TABLE "edit_lock" (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(30) NOT NULL, -- ENUM: PROJECT|PRESENTATION_CONTENT (폴리모픽, FK 없음)
    target_id BIGINT NOT NULL,
    locked_by VARCHAR(20) NOT NULL,
    locked_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_edit_lock_target UNIQUE (target_type, target_id),
    CONSTRAINT fk_edit_lock_locked_by FOREIGN KEY (locked_by) REFERENCES "user"(student_number)
);
COMMENT ON TABLE "edit_lock" IS '동시편집 잠금';
COMMENT ON COLUMN "edit_lock".id IS '식별자';
COMMENT ON COLUMN "edit_lock".target_type IS '대상 유형(PROJECT,PRESENTATION_CONTENT)';
COMMENT ON COLUMN "edit_lock".target_id IS '대상 식별자(폴리모픽, FK 아님)';
COMMENT ON COLUMN "edit_lock".locked_by IS '잠금 소유자 학번';
COMMENT ON COLUMN "edit_lock".locked_at IS '마지막 하트비트 시각';
COMMENT ON COLUMN "edit_lock".created_at IS '생성일';
COMMENT ON COLUMN "edit_lock".updated_at IS '수정일';
COMMENT ON COLUMN "edit_lock".deleted_at IS '삭제일';

-- =====================================================================
-- 끝 — 총 39개 테이블
-- 삭제 확정(2026-07-26): rubric, rubric_item, rubric_score — 이 스크립트에 없음.
-- 기존 ERDCloud 캔버스에 남아있다면 이 파일을 새 프로젝트로 import한 뒤 수동으로 지워주세요.
-- 2026-07-30 추가: edit_lock(동시편집 잠금, B2/B3 공유인프라) — 화면설계피드백(0729) 반영, 소유 파트 미정.
-- =====================================================================
