package kgu.developers.domain.meetingrecord.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import kgu.developers.domain.meetingrecord.domain.MeetingParticipant;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MeetingRecordRepositoryImpl implements MeetingRecordRepository {

    private final JpaMeetingRecordRepository jpaMeetingRecordRepository;
    private final JpaMeetingParticipantRepository jpaMeetingParticipantRepository;

    @Override
    public MeetingRecord save(MeetingRecord meetingRecord) {
        MeetingRecordJpaEntity savedEntity = jpaMeetingRecordRepository.save(MeetingRecordJpaEntity.toEntity(meetingRecord));

        jpaMeetingParticipantRepository.deleteAllByMeetingRecordId(savedEntity.getId());
        List<MeetingParticipant> savedParticipants = saveParticipants(savedEntity.getId(), meetingRecord.getParticipants());

        return savedEntity.toDomain(savedParticipants);
    }

    @Override
    public Optional<MeetingRecord> findById(Long id) {
        return jpaMeetingRecordRepository.findById(id)
            .map(entity -> entity.toDomain(findParticipants(id)));
    }

    @Override
    public List<MeetingRecord> findAllByTeamId(Long teamId, MeetingPhase phase) {
        List<MeetingRecordJpaEntity> entities = phase == null
            ? jpaMeetingRecordRepository.findAllByTeamId(teamId)
            : jpaMeetingRecordRepository.findAllByTeamIdAndPhase(teamId, phase);

        List<Long> meetingRecordIds = entities.stream().map(MeetingRecordJpaEntity::getId).toList();
        Map<Long, List<MeetingParticipant>> participantsByMeetingRecordId = jpaMeetingParticipantRepository.findAllByMeetingRecordIdIn(meetingRecordIds)
            .stream()
            .map(MeetingParticipantJpaEntity::toDomain)
            .collect(Collectors.groupingBy(MeetingParticipant::getMeetingRecordId));

        return entities.stream()
            .map(entity -> entity.toDomain(participantsByMeetingRecordId.getOrDefault(entity.getId(), List.of())))
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaMeetingParticipantRepository.deleteAllByMeetingRecordId(id);
        jpaMeetingRecordRepository.deleteById(id);
    }

    private List<MeetingParticipant> saveParticipants(Long meetingRecordId, List<MeetingParticipant> participants) {
        if (participants == null || participants.isEmpty()) {
            return List.of();
        }

        List<MeetingParticipantJpaEntity> entities = participants.stream()
            .map(participant -> MeetingParticipantJpaEntity.toEntity(
                MeetingParticipant.create(meetingRecordId, participant.getUserId())))
            .toList();

        return jpaMeetingParticipantRepository.saveAll(entities).stream()
            .map(MeetingParticipantJpaEntity::toDomain)
            .toList();
    }

    private List<MeetingParticipant> findParticipants(Long meetingRecordId) {
        return jpaMeetingParticipantRepository.findAllByMeetingRecordId(meetingRecordId).stream()
            .map(MeetingParticipantJpaEntity::toDomain)
            .toList();
    }
}
