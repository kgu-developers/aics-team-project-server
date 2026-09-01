package kgu.developers.api.auditlog.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record TeamHistoryPageResponse(
        @Schema(description = "팀 변경 이력", requiredMode = REQUIRED)
        List<TeamHistoryResponse> contents,
        @Schema(description = "페이지 정보", requiredMode = REQUIRED)
        PageableResponse<TeamHistoryResponse> pageable
) {
    public static TeamHistoryPageResponse from(Page<AuditLog> page, List<User> actors) {
        Map<String, User> actorsById = actors.stream()
                .collect(Collectors.toMap(User::getStudentNumber, Function.identity()));
        List<TeamHistoryResponse> contents = page.getContent().stream()
                .map(auditLog -> TeamHistoryResponse.from(auditLog, actorsById))
                .toList();
        PageableResponse<TeamHistoryResponse> pageable = PageableResponse.<TeamHistoryResponse>builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .isEnd(page.isLast())
                .build();
        return new TeamHistoryPageResponse(contents, pageable);
    }
}
