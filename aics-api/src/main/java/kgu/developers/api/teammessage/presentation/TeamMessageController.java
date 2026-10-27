package kgu.developers.api.teammessage.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.teammessage.presentation.request.SectionTeamMessageCreateRequest;
import kgu.developers.api.teammessage.presentation.request.TeamMessageCreateRequest;
import kgu.developers.api.teammessage.presentation.request.TeamMessageImportantUpdateRequest;
import kgu.developers.api.teammessage.presentation.response.SectionTeamMessagePersistResponse;
import kgu.developers.api.teammessage.presentation.response.TeamMessagePageResponse;
import kgu.developers.api.teammessage.presentation.response.TeamMessagePersistResponse;
import kgu.developers.api.teammessage.presentation.response.UnreadMessageCountResponse;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "TeamMessage", description = "팀 커뮤니케이션 메시지 API")
public interface TeamMessageController {

    @Operation(
        summary = "분반 전체 팀 메시지 등록 API",
        description = """
            Description : 담당 교수만 본인 분반의 삭제되지 않은 모든 팀에 동일한 GENERAL 메시지를 등록할 수 있다.
                팀 status(FORMING/CONFIRMED)는 가리지 않으므로 아직 구성 중인 팀에도 전달된다.
                발신자는 인증된 사용자로 기록하며, 팀별 스레드가 없으면 생성한다.
                관련 문서 상태는 변경하지 않고, 읽음 여부는 기존 팀 메시지와 동일하게 사용자별로 관리한다.
                전송 대상 팀이 없으면 sentCount는 0이다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = SectionTeamMessagePersistResponse.class)))
    @ApiResponse(responseCode = "403", description = "담당 교수가 아니거나 존재하지 않는/삭제된 분반")
    ResponseEntity<SectionTeamMessagePersistResponse> postSectionMessages(
        @PathVariable Long sectionId,
        @Valid @RequestBody SectionTeamMessageCreateRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "팀 메시지 목록 조회 API",
        description = """
            Description : 팀 스레드에 쌓인 메시지 히스토리를 관련 유형(relatedType)으로 필터링하여 페이지 단위로 조회한다.
                각 메시지의 read 값은 요청한 사용자 기준이다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TeamMessagePageResponse.class)))
    ResponseEntity<TeamMessagePageResponse> getMessages(
        @PathVariable Long teamId,
        @Parameter(description = "메시지 관련 유형 필터") @RequestParam(required = false) TeamMessageRelatedType relatedType,
        Pageable pageable,
        Authentication authentication
    );

    @Operation(
        summary = "팀 메시지 등록 API",
        description = """
            Description : 팀 스레드에 메시지(교수 피드백, 질문/답변, 학생의 QUESTION 호출 등)를 등록한다.
                스레드가 아직 없는 팀이라면 새로 생성한 뒤 등록한다. 발신자는 요청 값이 아니라 인증된 사용자로 기록된다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = TeamMessagePersistResponse.class)))
    ResponseEntity<TeamMessagePersistResponse> postMessage(
        @PathVariable Long teamId,
        @Valid @RequestBody TeamMessageCreateRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "팀 읽지 않은 메시지 수 조회 API",
        description = """
            Description : 팀 스레드에서 요청한 사용자가 아직 읽지 않은 메시지 개수를 조회한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UnreadMessageCountResponse.class)))
    ResponseEntity<UnreadMessageCountResponse> getUnreadCount(@PathVariable Long teamId, Authentication authentication);

    @Operation(
        summary = "메시지 중요 표시 변경 API",
        description = """
            Description : 메시지의 is_important 값을 요청 바디로 전달된 값으로 갱신한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "204")
    ResponseEntity<Void> updateImportant(
        @PathVariable Long id,
        @Valid @RequestBody TeamMessageImportantUpdateRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "메시지 읽음 처리 API",
        description = """
            Description : 요청한 사용자 기준으로 메시지를 읽음 처리한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "204")
    ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication);
}
