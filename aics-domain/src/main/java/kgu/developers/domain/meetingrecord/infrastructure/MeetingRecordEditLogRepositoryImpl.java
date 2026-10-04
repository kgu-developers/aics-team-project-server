package kgu.developers.domain.meetingrecord.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLogRepository;
import kgu.developers.domain.meetingrecord.infrastructure.JpaMeetingRecordEditLogRepository.EditCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MeetingRecordEditLogRepositoryImpl implements MeetingRecordEditLogRepository {

    private final JpaMeetingRecordEditLogRepository jpaMeetingRecordEditLogRepository;

    @Override
    public MeetingRecordEditLog save(MeetingRecordEditLog meetingRecordEditLog) {
        return jpaMeetingRecordEditLogRepository
            .save(MeetingRecordEditLogJpaEntity.toEntity(meetingRecordEditLog))
            .toDomain();
    }

    @Override
    public Page<MeetingRecordEditLog> findAllByMeetingRecordId(Long meetingRecordId, Pageable pageable) {
        return jpaMeetingRecordEditLogRepository.findAllByMeetingRecordId(meetingRecordId, pageable)
            .map(MeetingRecordEditLogJpaEntity::toDomain);
    }

    @Override
    public Page<MeetingRecordEditLog> findAllByTeamIdIn(
        List<Long> teamIds,
        Long meetingRecordId,
        Pageable pageable
    ) {
        if (teamIds == null || teamIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return jpaMeetingRecordEditLogRepository.findAllByTeamIdIn(teamIds, meetingRecordId, pageable)
            .map(MeetingRecordEditLogJpaEntity::toDomain);
    }

    @Override
    public Map<Long, Long> countByTeamIdInUntil(List<Long> teamIds, LocalDateTime until) {
        if (teamIds == null || teamIds.isEmpty() || until == null) {
            return Map.of();
        }
        return jpaMeetingRecordEditLogRepository.countByTeamIdInUntil(teamIds, until).stream()
            .collect(Collectors.toMap(EditCountProjection::getTeamId, EditCountProjection::getEditCount));
    }

    @Override
    public void deleteAllByMeetingRecordId(Long meetingRecordId) {
        jpaMeetingRecordEditLogRepository.deleteAllByMeetingRecordId(meetingRecordId);
    }
}
