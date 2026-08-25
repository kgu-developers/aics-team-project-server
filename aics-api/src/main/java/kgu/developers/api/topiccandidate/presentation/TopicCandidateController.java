package kgu.developers.api.topiccandidate.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidateListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "TopicCandidate", description = "주제 후보 API")
public interface TopicCandidateController {

    @Operation(
        summary = "주제 후보 목록 조회 API",
        description = """
            Description : 팀의 주제 후보 목록과 후보별 제안자, 설명, 득표 수, 현재 사용자의 투표 여부를 조회한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TopicCandidateListResponse.class)))
    ResponseEntity<TopicCandidateListResponse> getTopicCandidates(
        @PathVariable Long teamId,
        Authentication authentication
    );
}
