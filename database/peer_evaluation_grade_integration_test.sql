-- 실행 방법: peer_evaluation_grade.sql 적용 후 psql에서 이 파일을 실행한다.
-- 모든 검증 데이터는 마지막 ROLLBACK으로 제거된다.

BEGIN;

DO $$
DECLARE
    form_a_id BIGINT;
    form_b_id BIGINT;
    response_id BIGINT;
    question_id BIGINT;
BEGIN
    INSERT INTO peer_evaluation_form (
        section_id, milestone_id, is_anonymous, opens_at, closes_at
    ) VALUES (
        1, 1, FALSE, TIMESTAMP '2026-08-01 09:00:00', TIMESTAMP '2026-08-07 18:00:00'
    ) RETURNING id INTO form_a_id;

    INSERT INTO peer_evaluation_form (
        section_id, milestone_id, is_anonymous, opens_at, closes_at
    ) VALUES (
        1, 2, FALSE, TIMESTAMP '2026-08-08 09:00:00', TIMESTAMP '2026-08-14 18:00:00'
    ) RETURNING id INTO form_b_id;

    INSERT INTO peer_evaluation_response (
        form_id, evaluator_id, target_id
    ) VALUES (
        form_a_id, 'it-evaluator', 'it-target'
    ) RETURNING id INTO response_id;

    INSERT INTO peer_evaluation_question (
        form_id, text, type, max_score, display_order
    ) VALUES (
        form_a_id, '통합 검증 질문', 'TEXT', NULL, 0
    ) RETURNING id INTO question_id;

    INSERT INTO peer_evaluation_answer (
        response_id, question_id, score, text_answer
    ) VALUES (
        response_id, question_id, NULL, '통합 검증 답변'
    );

    BEGIN
        UPDATE peer_evaluation_response
        SET form_id = form_b_id
        WHERE id = response_id;
        RAISE EXCEPTION '응답의 form_id 변경이 거부되지 않았습니다.';
    EXCEPTION
        WHEN check_violation THEN NULL;
    END;

    BEGIN
        UPDATE peer_evaluation_question
        SET form_id = form_b_id
        WHERE id = question_id;
        RAISE EXCEPTION '질문의 form_id 변경이 거부되지 않았습니다.';
    EXCEPTION
        WHEN check_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO grade (
            section_id, team_id, user_id, finalized_at
        ) VALUES (
            1, 1, 'it-grade', CURRENT_TIMESTAMP
        );
        RAISE EXCEPTION '불완전한 확정 성적이 거부되지 않았습니다.';
    EXCEPTION
        WHEN check_violation THEN NULL;
    END;
END;
$$;

ROLLBACK;
