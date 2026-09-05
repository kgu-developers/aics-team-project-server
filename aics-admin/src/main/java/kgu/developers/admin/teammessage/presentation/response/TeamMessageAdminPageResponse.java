package kgu.developers.admin.teammessage.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.teamthread.exception.TeamThreadNotFoundException;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record TeamMessageAdminPageResponse(

    @Schema(description = "메시지 목록", requiredMode = REQUIRED)
    List<TeamMessageAdminResponse> contents,

    @Schema(description = "조회 범위의 읽지 않은 메시지 수", example = "12", requiredMode = REQUIRED)
    long unreadCount,

    @Schema(description = "페이지 정보", requiredMode = REQUIRED)
    PageableResponse<TeamMessageAdminResponse> pageable
) {
    public static TeamMessageAdminPageResponse from(
        Page<TeamMessage> page,
        long unreadCount,
        Set<Long> readMessageIds,
        Map<Long, TeamThread> threadsById,
        Map<Long, Team> teamsById,
        Map<Long, Section> sectionsById
    ) {
        List<TeamMessageAdminResponse> contents = page.getContent().stream()
            .map(message -> {
                TeamThread thread = Optional.ofNullable(threadsById.get(message.getThreadId()))
                    .orElseThrow(TeamThreadNotFoundException::new);
                Team team = Optional.ofNullable(teamsById.get(thread.getTeamId()))
                    .orElseThrow(TeamNotFoundException::new);
                Section section = Optional.ofNullable(sectionsById.get(team.getSectionId()))
                    .orElseThrow(SectionNotFoundException::new);
                return TeamMessageAdminResponse.from(
                    message, team, section, readMessageIds.contains(message.getId()));
            })
            .toList();

        PageableResponse<TeamMessageAdminResponse> pageable = PageableResponse.<TeamMessageAdminResponse>builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalPages(page.getTotalPages())
            .totalElements(page.getTotalElements())
            .isEnd(page.isLast())
            .build();

        return TeamMessageAdminPageResponse.builder()
            .contents(contents)
            .unreadCount(unreadCount)
            .pageable(pageable)
            .build();
    }
}
