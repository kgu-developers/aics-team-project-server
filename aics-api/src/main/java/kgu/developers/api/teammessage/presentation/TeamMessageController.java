package kgu.developers.api.teammessage.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.teammessage.presentation.request.TeamMessageCreateRequest;
import kgu.developers.api.teammessage.presentation.request.TeamMessageImportantUpdateRequest;
import kgu.developers.api.teammessage.presentation.response.TeamMessagePageResponse;
import kgu.developers.api.teammessage.presentation.response.TeamMessagePersistResponse;
import kgu.developers.api.teammessage.presentation.response.UnreadMessageCountResponse;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "TeamMessage", description = "팀 커뮤니케이션 메시지 API")
public interface TeamMessageController {

    @Operation(
        summary = "팀 메시지 목록 조회 API",
        description = """
            Description : 팀 스레드에 쌓인 메시지 히스토리를 관련 유형(relatedType)으로 필터링하여 페이지 단위로 조회한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TeamMessagePageResponse.class)))
    ResponseEntity<TeamMessagePageResponse> getMessages(
        @PathVariable Long teamId,
        @Parameter(description = "메시지 관련 유형 필터") @RequestParam(required = false) TeamMessageRelatedType relatedType,
        Pageable pageable
    );

    @Operation(
        summary = "팀 메시지 등록 API",
        description = """
            Description : 팀 스레드에 메시지(교수 피드백, 질문/답변, 학생의 QUESTION 호출 등)를 등록한다.
                스레드가 아직 없는 팀이라면 새로 생성한 뒤 등록한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = TeamMessagePersistResponse.class)))
    ResponseEntity<TeamMessagePersistResponse> postMessage(@PathVariable Long teamId, @Valid @RequestBody TeamMessageCreateRequest request);

    @Operation(
        summary = "팀 읽지 않은 메시지 수 조회 API",
        description = """
            Description : 팀 스레드에서 is_read = false 인 메시지 개수를 조회한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UnreadMessageCountResponse.class)))
    ResponseEntity<UnreadMessageCountResponse> getUnreadCount(@PathVariable Long teamId);

    @Operation(
        summary = "메시지 중요 표시 변경 API",
        description = """
            Description : 메시지의 is_important 값을 요청 바디로 전달된 값으로 갱신한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "204")
    ResponseEntity<Void> updateImportant(@PathVariable Long id, @Valid @RequestBody TeamMessageImportantUpdateRequest request);

    @Operation(
        summary = "메시지 읽음 처리 API",
        description = """
            Description : 메시지의 is_read 값을 true로 갱신한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "204")
    ResponseEntity<Void> markAsRead(@PathVariable Long id);
}
