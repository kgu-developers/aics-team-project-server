package kgu.developers.api.milestone.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kgu.developers.api.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.api.milestone.presentation.response.RequiredArtifactListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Milestone", description = "학생 마일스톤 API")
public interface MilestoneController {

    @Operation(
        summary = "학생용 분반 마일스톤 목록 조회 API",
        description = """
            Description : 인증된 학생이 수강 중인 분반의 공개 마일스톤 목록을 주차순으로 조회한다.
                          DRAFT 상태는 노출하지 않고 PUBLISHED, CLOSED 상태만 반환한다.
                          응답의 id를 사용해 /milestones/{milestoneId}/my-team-submission을 조회할 수 있다.
                          PRESENTATION은 dueAt으로 발표자료 제출 마감을, evaluationOpensAt/evaluationClosesAt으로 발표 평가 기간을 구분한다.
            Assignee : 최태양
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MilestoneListResponse.class)))
    ResponseEntity<MilestoneListResponse> getMilestones(
        @Positive @PathVariable Long sectionId,
        Authentication authentication
    );

    @Operation(
        summary = "학생용 마일스톤 필수 산출물 목록 조회 API",
        description = """
            Description : 인증된 학생이 수강 중인 분반의 공개 마일스톤에 설정된 제출 항목을 조회한다.
                          응답의 id를 제출 API의 requiredArtifactId로 사용한다.
                          type은 FILE, LINK, TEXT, CHEERPJ_RUN 중 하나이며 파일 옵션은 FILE에서만 제공된다.
            Assignee : 최태양
            """
    )
    ResponseEntity<RequiredArtifactListResponse> getRequiredArtifacts(
        @Positive @PathVariable Long sectionId,
        @Positive @PathVariable Long milestoneId,
        Authentication authentication
    );
}
