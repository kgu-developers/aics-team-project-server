package kgu.developers.api.evaluation.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.evaluation.presentation.request.PeerEvaluationResponseRequest;
import kgu.developers.api.evaluation.presentation.response.MyPeerEvaluationResponse;
import kgu.developers.api.evaluation.presentation.response.PeerEvaluationTargetsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "PeerEvaluation", description = "학생 상호평가 API")
public interface PeerEvaluationController {
    @Operation(
        summary = "상호평가 폼과 대상 조회",
        description = "활성 STUDENT의 같은 팀원 중 본인, 탈퇴 학생, 조교를 제외해 반환합니다. windowState enum은 UPCOMING, OPEN, CLOSED입니다."
    )
    ResponseEntity<PeerEvaluationTargetsResponse> getTargets(
        @PathVariable Long formId,
        Authentication authentication
    );

    @Operation(
        summary = "상호평가 임시저장 또는 제출",
        description = "submit=false는 임시저장, submit=true는 최종 제출입니다. 최종 제출 시 본인을 제외한 활성 STUDENT 팀원 전체를 한 번씩 평가하고 기여도 합계가 100이어야 합니다. 제출 완료 후에는 수정할 수 없습니다."
    )
    ResponseEntity<MyPeerEvaluationResponse> submitResponse(
        @PathVariable Long formId,
        @Valid @RequestBody PeerEvaluationResponseRequest request,
        Authentication authentication
    );
}
