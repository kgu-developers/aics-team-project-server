package kgu.developers.api.evaluation.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.evaluation.presentation.request.TeamEvaluationSubmitRequest;
import kgu.developers.api.evaluation.presentation.response.MyTeamEvaluationsResponse;
import kgu.developers.api.evaluation.presentation.response.TeamEvaluationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "TeamEvaluation", description = "학생 발표 평가 API")
public interface TeamEvaluationController {
    @Operation(
            summary = "내 발표 평가 현황 조회",
            description = "활성 STUDENT가 PRESENTATION 마일스톤의 평가 기간, 평가 항목과 본인이 저장한 팀별 평가를 조회합니다. windowState enum은 UNAVAILABLE, UPCOMING, OPEN, CLOSED입니다. 발표 대상 팀은 GET /milestones/{milestoneId}/presentations를 사용합니다."
    )
    ResponseEntity<MyTeamEvaluationsResponse> getMyEvaluations(
            @PathVariable Long milestoneId,
            Authentication authentication
    );

    @Operation(
            summary = "팀 발표 평가 제출",
            description = "평가 기간 중 같은 분반의 다른 팀에 대해 활성 평가 항목 전체의 점수를 제출합니다. 각 score는 0 이상 maxScore 이하여야 합니다. 같은 학생이 같은 팀에 다시 요청하면 기존 평가를 갱신합니다."
    )
    ResponseEntity<TeamEvaluationResponse> submit(
            @PathVariable Long milestoneId,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamEvaluationSubmitRequest request,
            Authentication authentication
    );
}
