package kgu.developers.api.meetingrecord.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordEditLogPageResponse;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordEditLogQueryService;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MeetingRecordEditLogFacade {

    private static final Sort LATEST_FIRST = Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("id")
    );

    private final MeetingRecordQueryService meetingRecordQueryService;
    private final MeetingRecordEditLogQueryService meetingRecordEditLogQueryService;
    private final TeamAccessValidator teamAccessValidator;
    private final UserQueryService userQueryService;

    public MeetingRecordEditLogPageResponse getMeetingRecordLogs(Long meetingRecordId, Pageable pageable, String userId) {
        MeetingRecord meetingRecord = meetingRecordQueryService.getMeetingRecord(meetingRecordId);
        // 수정 사유는 팀원과 담당 교수가 함께 본다(9.29 미팅 확정).
        teamAccessValidator.validateMembershipOrProfessor(meetingRecord.getTeamId(), userId);

        Page<MeetingRecordEditLog> logs = meetingRecordEditLogQueryService.getMeetingRecordLogs(
            meetingRecordId,
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), LATEST_FIRST)
        );

        return MeetingRecordEditLogPageResponse.from(logs, editorsByStudentNumber(logs.getContent()));
    }

    private Map<String, User> editorsByStudentNumber(List<MeetingRecordEditLog> logs) {
        List<String> editorIds = logs.stream()
            .map(MeetingRecordEditLog::getEditorId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (editorIds.isEmpty()) {
            return Map.of();
        }
        // 수정자가 탈퇴해도 로그는 회의록에 계속 남는다. 활성 사용자만 조회하면 과거 감사 이력의
        // 작성자 이름이 사라지므로, 제출 이력·쪽지함과 같이 탈퇴 사용자까지 포함해 조회한다.
        return userQueryService.getUsersByStudentNumbersIncludingDeleted(editorIds).stream()
            .collect(Collectors.toMap(User::getStudentNumber, Function.identity()));
    }
}
