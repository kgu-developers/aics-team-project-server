package kgu.developers.api.editlock.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.editlock.presentation.request.EditLockAcquireRequest;
import kgu.developers.api.editlock.presentation.response.EditLockStatusResponse;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "EditLock", description = "동시편집 잠금 API(공유 인프라, B2 Project·B3 PresentationContent 등에서 재사용)")
public interface EditLockController {

    @Operation(
        summary = "잠금 상태 조회 API",
        description = """
            Description : 지금 편집 중인 사람이 있는지 조회한다. 만료된 잠금(TTL 경과)은 없는 것으로 취급한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = EditLockStatusResponse.class)))
    ResponseEntity<EditLockStatusResponse> getStatus(
        @RequestParam EditLockTargetType targetType,
        @RequestParam Long targetId,
        Authentication authentication
    );

    @Operation(
        summary = "잠금 획득/하트비트 API",
        description = """
            Description : 잠금이 없으면 새로 획득하고, 본인 소유면 갱신(하트비트)한다. 타인이 살아있게 잠그고 있으면 409.
            편집 화면 진입 시 최초 호출하고, 편집 중 30초~1분 주기로 재호출한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = EditLockStatusResponse.class)))
    @ApiResponse(responseCode = "409", description = "타인이 편집 중")
    ResponseEntity<EditLockStatusResponse> acquire(
        @Valid @RequestBody EditLockAcquireRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "잠금 해제 API",
        description = """
            Description : 본인 소유 잠금만 해제한다. 없거나 타인 소유면 아무 일도 하지 않는다(멱등).
            저장 성공 또는 편집 취소 시 호출한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "204")
    ResponseEntity<Void> release(
        @RequestParam EditLockTargetType targetType,
        @RequestParam Long targetId,
        Authentication authentication
    );
}
