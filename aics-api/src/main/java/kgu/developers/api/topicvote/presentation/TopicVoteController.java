package kgu.developers.api.topicvote.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kgu.developers.api.topicvote.presentation.response.TopicVotePersistResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "TopicVote", description = "주제 후보 투표 API")
public interface TopicVoteController {

    @Operation(
        summary = "주제 후보 투표 API",
        description = """
            Description : 팀원은 팀당 하나의 주제 후보에 투표할 수 있다. 이미 투표한 상태에서 다른 후보에 요청하면
                기존 투표가 새 후보로 변경된다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = TopicVotePersistResponse.class)))
    ResponseEntity<TopicVotePersistResponse> vote(@PathVariable Long candidateId, Authentication authentication);

    @Operation(
        summary = "주제 후보 투표 취소 API",
        description = """
            Description : 팀원이 해당 주제 후보에 행사한 투표를 취소한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "204")
    ResponseEntity<Void> cancelVote(@PathVariable Long candidateId, Authentication authentication);
}
