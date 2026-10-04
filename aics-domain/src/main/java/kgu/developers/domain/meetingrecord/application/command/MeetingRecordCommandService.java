package kgu.developers.domain.meetingrecord.application.command;

import java.time.LocalDateTime;
import java.util.List;
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
        if (participantIds != null && !participantUserIds(meetingRecord).equals(
            MeetingRecord.toParticipants(meetingRecord.getId(), participantIds).stream()
                .map(MeetingParticipant::getUserId)
                .toList())) {
            meetingRecord.updateParticipants(participantIds);
            changed = true;
        }
        if (milestoneIds != null && !meetingRecord.getMilestoneIds().equals(
            MeetingRecord.normalizeMilestoneIds(milestoneIds))) {
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
