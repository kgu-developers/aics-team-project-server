package kgu.developers.domain.meetingrecord.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final JpaMeetingActionRepository jpaMeetingActionRepository;

    @Override
    public MeetingRecord save(MeetingRecord meetingRecord) {
        if (meetingRecord.getId() != null) {
            jpaMeetingRecordRepository.findByIdForUpdate(meetingRecord.getId());
        }

        MeetingRecordJpaEntity savedEntity = jpaMeetingRecordRepository.save(MeetingRecordJpaEntity.toEntity(meetingRecord));

        List<MeetingParticipant> savedParticipants = syncParticipants(savedEntity.getId(), meetingRecord.getParticipants());

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
        jpaMeetingActionRepository.deleteAllByMeetingRecordId(id);
        jpaMeetingRecordRepository.deleteById(id);
    }

    private List<MeetingParticipant> syncParticipants(Long meetingRecordId, List<MeetingParticipant> participants) {
        List<MeetingParticipant> incoming = participants == null ? List.of() : participants;
        List<MeetingParticipantJpaEntity> existingEntities = jpaMeetingParticipantRepository.findAllByMeetingRecordId(meetingRecordId);

        Set<String> incomingUserIds = incoming.stream()
            .map(MeetingParticipant::getUserId)
            .collect(Collectors.toSet());
        Set<String> existingUserIds = existingEntities.stream()
            .map(MeetingParticipantJpaEntity::getUserId)
            .collect(Collectors.toSet());

        List<Long> idsToRemove = existingEntities.stream()
            .filter(entity -> !incomingUserIds.contains(entity.getUserId()))
            .map(MeetingParticipantJpaEntity::getId)
            .toList();
        if (!idsToRemove.isEmpty()) {
            jpaMeetingParticipantRepository.deleteAllById(idsToRemove);
        }

        List<MeetingParticipantJpaEntity> entitiesToAdd = incoming.stream()
            .filter(participant -> !existingUserIds.contains(participant.getUserId()))
            .map(participant -> MeetingParticipantJpaEntity.toEntity(
                MeetingParticipant.create(meetingRecordId, participant.getUserId())))
            .toList();
        if (!entitiesToAdd.isEmpty()) {
            jpaMeetingParticipantRepository.saveAll(entitiesToAdd);
        }

        return findParticipants(meetingRecordId);
    }

    private List<MeetingParticipant> findParticipants(Long meetingRecordId) {
        return jpaMeetingParticipantRepository.findAllByMeetingRecordId(meetingRecordId).stream()
            .map(MeetingParticipantJpaEntity::toDomain)
            .toList();
    }
}
