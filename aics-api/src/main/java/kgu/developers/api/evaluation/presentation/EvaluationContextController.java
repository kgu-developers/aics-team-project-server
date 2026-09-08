package kgu.developers.api.evaluation.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kgu.developers.api.evaluation.presentation.response.EvaluationContextResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Evaluation", description = "학생 평가 화면 진입 API")
public interface EvaluationContextController {
    @Operation(
        summary = "학생 평가 화면 컨텍스트 조회",
        description = "활성 STUDENT의 분반에서 공개된 PRESENTATION 마일스톤 ID와 상호평가 양식 ID를 조회합니다. 아직 설정되지 않은 항목은 null입니다."
    )
    ResponseEntity<EvaluationContextResponse> getContext(
        @PathVariable Long sectionId,
        Authentication authentication
    );
}
