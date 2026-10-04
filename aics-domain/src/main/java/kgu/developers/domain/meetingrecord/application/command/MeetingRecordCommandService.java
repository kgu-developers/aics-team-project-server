package kgu.developers.domain.meetingrecord.application.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import kgu.developers.domain.meetingrecord.domain.MeetingParticipant;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLogRepository;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordRepository;
import kgu.developers.domain.meetingrecord.exception.MeetingRecordInvalidContentException;
import kgu.developers.domain.meetingrecord.exception.MeetingRecordInvalidTitleException;
import kgu.developers.domain.meetingrecord.exception.MeetingRecordNoUpdateFieldException;
import kgu.developers.domain.meetingrecord.exception.MeetingRecordNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MeetingRecordCommandService {

    private final MeetingRecordRepository meetingRecordRepository;
    private final MeetingRecordEditLogRepository meetingRecordEditLogRepository;

    public Long createMeetingRecord(
        Long teamId,
        String title,
        MeetingPhase phase,
        String authorId,
        LocalDateTime meetingAt,
        String location,
        String content,
        List<String> participantIds
    ) {
        return createMeetingRecord(
            teamId, title, phase, authorId, meetingAt, location, content, participantIds, List.of());
    }

    public Long createMeetingRecord(
        Long teamId,
        String title,
        MeetingPhase phase,
        String authorId,
        LocalDateTime meetingAt,
        String location,
        String content,
        List<String> participantIds,
        List<Long> milestoneIds
    ) {
        MeetingRecord meetingRecord = MeetingRecord.create(
            teamId, title, phase, authorId, meetingAt, location, content, participantIds, milestoneIds);
        return meetingRecordRepository.save(meetingRecord).getId();
    }

    public void updateMeetingRecord(
        Long id,
        String title,
        LocalDateTime meetingAt,
        String location,
        MeetingPhase phase,
        String content,
        List<String> participantIds
    ) {
        updateMeetingRecord(id, title, meetingAt, location, phase, content, participantIds, null);
    }

    public void updateMeetingRecord(
        Long id,
        String title,
        LocalDateTime meetingAt,
        String location,
        MeetingPhase phase,
        String content,
        List<String> participantIds,
        List<Long> milestoneIds
    ) {
        updateMeetingRecord(id, title, meetingAt, location, phase, content, participantIds, milestoneIds, null, null);
    }

    // 수정 사유(reason)가 주어지면 같은 트랜잭션에서 수정 이력을 한 건 적재한다.
    // 사유 검증이 실패하면 회의록 변경도 함께 롤백돼, 사유 없는 수정이 남지 않는다.
    public void updateMeetingRecord(
        Long id,
        String title,
        LocalDateTime meetingAt,
        String location,
        MeetingPhase phase,
        String content,
        List<String> participantIds,
        List<Long> milestoneIds,
        String reason,
        String editorId
    ) {
        // 사유만 보내고 바꿀 값은 하나도 없는 요청은 거절한다. 그대로 두면 수정하지 않은 기록이
        // 이력에 쌓이고, 분반 산출물 엑셀의 '회의록 수정 수'가 실제보다 부풀려진다.
        if (title == null && meetingAt == null && location == null && phase == null
            && content == null && participantIds == null && milestoneIds == null) {
            throw new MeetingRecordNoUpdateFieldException();
        }

        // 실제 변경이 없어 로그를 남기지 않는 요청도 같은 사유 규칙을 적용받아야 한다.
        // 조기 반환 뒤에서 검증하면 "같은 값 + 짧은 사유"만 통과해 API 규칙이 요청에 따라 달라진다.
        if (reason != null) {
            MeetingRecordEditLog.validateReason(reason);
        }

        MeetingRecord meetingRecord = findOrThrow(id);
        boolean changed = false;

        if (title != null) {
            if (title.isBlank()) {
                throw new MeetingRecordInvalidTitleException();
            }
            if (!title.equals(meetingRecord.getTitle())) {
                meetingRecord.updateTitle(title);
                changed = true;
            }
        }
        if (meetingAt != null && !meetingAt.equals(meetingRecord.getMeetingAt())) {
            meetingRecord.updateMeetingAt(meetingAt);
            changed = true;
        }
        if (location != null && !location.equals(meetingRecord.getLocation())) {
            meetingRecord.updateLocation(location);
            changed = true;
        }
        if (phase != null && phase != meetingRecord.getPhase()) {
            meetingRecord.updatePhase(phase);
            changed = true;
        }
        if (content != null) {
            if (content.isBlank()) {
                throw new MeetingRecordInvalidContentException();
            }
            if (!content.equals(meetingRecord.getContent())) {
                meetingRecord.updateContent(content);
                changed = true;
            }
        }
        // 참석자·마일스톤은 리포지토리가 집합 기준으로 동기화하므로(syncParticipants/syncMilestones)
        // 순서만 바꿔 보낸 요청은 실제 연결이 그대로다. 비교도 집합으로 해야 "안 바꾼 수정"이 안 쌓인다.
        if (participantIds != null && !Set.copyOf(participantUserIds(meetingRecord)).equals(
            Set.copyOf(MeetingRecord.toParticipants(meetingRecord.getId(), participantIds).stream()
                .map(MeetingParticipant::getUserId)
                .toList()))) {
            meetingRecord.updateParticipants(participantIds);
            changed = true;
        }
        if (milestoneIds != null && !Set.copyOf(meetingRecord.getMilestoneIds()).equals(
            Set.copyOf(MeetingRecord.normalizeMilestoneIds(milestoneIds)))) {
            meetingRecord.updateMilestoneIds(milestoneIds);
            changed = true;
        }

        // 같은 값을 다시 보낸 요청은 실제로 바뀐 게 없으므로 저장도, 이력 적재도 하지 않는다.
        if (!changed) {
            return;
        }

        meetingRecordRepository.save(meetingRecord);

        if (reason != null) {
            meetingRecordEditLogRepository.save(MeetingRecordEditLog.create(
                meetingRecord.getId(), meetingRecord.getTeamId(), editorId, reason));
        }
    }

    // TODO: 정책이 소프트 삭제로 바뀌면 BaseTimeEntity.delete() 기반으로 전환한다.
    public void deleteMeetingRecord(Long id) {
        findOrThrow(id);
        meetingRecordRepository.deleteById(id);
    }

    private List<String> participantUserIds(MeetingRecord meetingRecord) {
        return meetingRecord.getParticipants() == null
            ? List.of()
            : meetingRecord.getParticipants().stream().map(MeetingParticipant::getUserId).toList();
    }

    private MeetingRecord findOrThrow(Long id) {
        return meetingRecordRepository.findById(id)
            .orElseThrow(MeetingRecordNotFoundException::new);
    }
}
