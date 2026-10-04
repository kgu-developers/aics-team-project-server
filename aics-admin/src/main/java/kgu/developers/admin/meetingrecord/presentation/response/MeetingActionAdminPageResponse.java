package kgu.developers.admin.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record MeetingActionAdminPageResponse(

    @Schema(description = "액션플랜 목록", requiredMode = REQUIRED)
    List<MeetingActionAdminResponse> contents,

    @Schema(description = "페이지 정보", requiredMode = REQUIRED)
    PageableResponse<MeetingActionAdminResponse> pageable
) {
    public static MeetingActionAdminPageResponse from(
        Page<MeetingAction> page,
        Map<Long, MeetingRecord> meetingRecordsById,
        Map<Long, Team> teamsById,
        Map<String, User> usersByStudentNumber
    ) {
        // 액션플랜 페이지를 읽은 뒤 회의록 조회 사이에 학생이 그 회의록을 지우면(하드 삭제라
        // 액션플랜도 같이 사라진다) 조립할 회의록·팀이 비어버린다. 읽기 트랜잭션이라고 해서
        // 두 조회가 같은 스냅샷을 보는 건 아니므로, 사라진 행은 응답에서 빼고 500으로 깨지지 않게 한다.
        List<MeetingActionAdminResponse> contents = page.getContent().stream()
            .flatMap(meetingAction -> {
                MeetingRecord meetingRecord = meetingRecordsById.get(meetingAction.getMeetingRecordId());
                if (meetingRecord == null) {
                    return Stream.<MeetingActionAdminResponse>empty();
                }
                Team team = teamsById.get(meetingRecord.getTeamId());
                if (team == null) {
                    return Stream.<MeetingActionAdminResponse>empty();
                }
                User assignee = meetingAction.getAssigneeId() == null
                    ? null
                    : usersByStudentNumber.get(meetingAction.getAssigneeId());
                return Stream.of(MeetingActionAdminResponse.from(meetingAction, meetingRecord, team, assignee));
            })
            .toList();

        PageableResponse<MeetingActionAdminResponse> pageable = PageableResponse.<MeetingActionAdminResponse>builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalPages(page.getTotalPages())
            .totalElements(page.getTotalElements())
            .isEnd(page.isLast())
            .build();

        return MeetingActionAdminPageResponse.builder()
            .contents(contents)
            .pageable(pageable)
            .build();
    }
}
