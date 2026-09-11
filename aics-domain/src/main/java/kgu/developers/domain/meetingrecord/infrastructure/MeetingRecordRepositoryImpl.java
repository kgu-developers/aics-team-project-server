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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MeetingRecordRepositoryImpl implements MeetingRecordRepository {

    private final JpaMeetingRecordRepository jpaMeetingRecordRepository;
    private final JpaMeetingParticipantRepository jpaMeetingParticipantRepository;
    private final JpaMeetingActionRepository jpaMeetingActionRepository;
    private final JpaMeetingRecordMilestoneRepository jpaMeetingRecordMilestoneRepository;

    @Override
    public MeetingRecord save(MeetingRecord meetingRecord) {
        if (meetingRecord.getId() != null) {
            jpaMeetingRecordRepository.findByIdForUpdate(meetingRecord.getId());
        }

        MeetingRecordJpaEntity savedEntity = jpaMeetingRecordRepository.save(MeetingRecordJpaEntity.toEntity(meetingRecord));

        List<MeetingParticipant> savedParticipants = syncParticipants(savedEntity.getId(), meetingRecord.getParticipants());
        List<Long> savedMilestoneIds = syncMilestones(savedEntity.getId(), meetingRecord.getMilestoneIds());

        return savedEntity.toDomain(savedParticipants, savedMilestoneIds);
    }

    @Override
    public Optional<MeetingRecord> findById(Long id) {
        return jpaMeetingRecordRepository.findById(id)
            .map(entity -> entity.toDomain(findParticipants(id), findMilestoneIds(id)));
    }

    @Override
    public List<MeetingRecord> findAllByTeamId(Long teamId, MeetingPhase phase) {
        List<MeetingRecordJpaEntity> entities = phase == null
            ? jpaMeetingRecordRepository.findAllByTeamId(teamId)
            : jpaMeetingRecordRepository.findAllByTeamIdAndPhase(teamId, phase);

        List<Long> meetingRecordIds = entities.stream().map(MeetingRecordJpaEntity::getId).toList();
        Map<Long, List<MeetingParticipant>> participantsByMeetingRecordId =
            findParticipantsByMeetingRecordId(meetingRecordIds);
        Map<Long, List<Long>> milestoneIdsByMeetingRecordId = findMilestoneIdsByMeetingRecordId(meetingRecordIds);

        return entities.stream()
            .map(entity -> entity.toDomain(
                participantsByMeetingRecordId.getOrDefault(entity.getId(), List.of()),
                milestoneIdsByMeetingRecordId.getOrDefault(entity.getId(), List.of())))
            .toList();
    }

    @Override
    public List<MeetingRecord> findAllByIdIn(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        List<MeetingRecordJpaEntity> entities = jpaMeetingRecordRepository.findAllById(ids);
        List<Long> meetingRecordIds = entities.stream().map(MeetingRecordJpaEntity::getId).toList();
        Map<Long, List<MeetingParticipant>> participantsByMeetingRecordId =
            findParticipantsByMeetingRecordId(meetingRecordIds);
        Map<Long, List<Long>> milestoneIdsByMeetingRecordId = findMilestoneIdsByMeetingRecordId(meetingRecordIds);

        return entities.stream()
            .map(entity -> entity.toDomain(
                participantsByMeetingRecordId.getOrDefault(entity.getId(), List.of()),
                milestoneIdsByMeetingRecordId.getOrDefault(entity.getId(), List.of())))
            .toList();
    }

    @Override
    public Page<MeetingRecord> findAllByTeamIdIn(List<Long> teamIds, Pageable pageable) {
        if (teamIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<MeetingRecordJpaEntity> entities = jpaMeetingRecordRepository.findAllByTeamIdIn(teamIds, pageable);
        return toDomainPage(entities);
    }

    @Override
    public Page<MeetingRecord> findAllByTeamIdInAndMilestoneId(
        List<Long> teamIds,
        Long milestoneId,
        Pageable pageable
    ) {
        if (teamIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return toDomainPage(jpaMeetingRecordRepository.findAllByTeamIdInAndMilestoneId(
            teamIds, milestoneId, pageable));
    }

    @Override
    public long countByTeamIdAndMilestoneId(Long teamId, Long milestoneId) {
        return jpaMeetingRecordRepository.countByTeamIdAndMilestoneId(teamId, milestoneId);
    }

    @Override
    public Map<Long, Long> countByTeamIdInAndMilestoneId(List<Long> teamIds, Long milestoneId) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        return jpaMeetingRecordRepository.countByTeamIdInAndMilestoneId(teamIds, milestoneId).stream()
            .collect(Collectors.toMap(
                JpaMeetingRecordRepository.MeetingRecordCountProjection::getTeamId,
                JpaMeetingRecordRepository.MeetingRecordCountProjection::getMeetingRecordCount
            ));
    }

    @Override
    public Map<Long, Long> countByTeamIdIn(List<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        return jpaMeetingRecordRepository.countByTeamIdIn(teamIds).stream()
            .collect(Collectors.toMap(
                JpaMeetingRecordRepository.MeetingRecordCountProjection::getTeamId,
                JpaMeetingRecordRepository.MeetingRecordCountProjection::getMeetingRecordCount
            ));
    }

    private Page<MeetingRecord> toDomainPage(Page<MeetingRecordJpaEntity> entities) {
        List<Long> meetingRecordIds = entities.stream().map(MeetingRecordJpaEntity::getId).toList();
        Map<Long, List<MeetingParticipant>> participantsByMeetingRecordId = findParticipantsByMeetingRecordId(
            meetingRecordIds);
        Map<Long, List<Long>> milestoneIdsByMeetingRecordId = findMilestoneIdsByMeetingRecordId(meetingRecordIds);

        return entities.map(entity -> entity.toDomain(
            participantsByMeetingRecordId.getOrDefault(entity.getId(), List.of()),
            milestoneIdsByMeetingRecordId.getOrDefault(entity.getId(), List.of())));
    }

    @Override
    public void deleteById(Long id) {
        jpaMeetingParticipantRepository.deleteAllByMeetingRecordId(id);
        jpaMeetingRecordMilestoneRepository.deleteAllByMeetingRecordId(id);
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

    private List<Long> syncMilestones(Long meetingRecordId, List<Long> milestoneIds) {
        List<Long> incoming = MeetingRecord.normalizeMilestoneIds(milestoneIds);
        List<MeetingRecordMilestoneJpaEntity> existing =
            jpaMeetingRecordMilestoneRepository.findAllByMeetingRecordId(meetingRecordId);
        Set<Long> incomingIds = Set.copyOf(incoming);
        Set<Long> existingIds = existing.stream()
            .map(MeetingRecordMilestoneJpaEntity::getMilestoneId)
            .collect(Collectors.toSet());

        List<Long> linkIdsToRemove = existing.stream()
            .filter(link -> !incomingIds.contains(link.getMilestoneId()))
            .map(MeetingRecordMilestoneJpaEntity::getId)
            .toList();
        if (!linkIdsToRemove.isEmpty()) {
            jpaMeetingRecordMilestoneRepository.deleteAllById(linkIdsToRemove);
        }

        List<MeetingRecordMilestoneJpaEntity> linksToAdd = incoming.stream()
            .filter(milestoneId -> !existingIds.contains(milestoneId))
            .map(milestoneId -> MeetingRecordMilestoneJpaEntity.builder()
                .meetingRecordId(meetingRecordId)
                .milestoneId(milestoneId)
                .build())
            .toList();
        if (!linksToAdd.isEmpty()) {
            jpaMeetingRecordMilestoneRepository.saveAll(linksToAdd);
        }
        return findMilestoneIds(meetingRecordId);
    }

    private List<Long> findMilestoneIds(Long meetingRecordId) {
        return jpaMeetingRecordMilestoneRepository.findAllByMeetingRecordId(meetingRecordId).stream()
            .map(MeetingRecordMilestoneJpaEntity::getMilestoneId)
            .toList();
    }

    private Map<Long, List<Long>> findMilestoneIdsByMeetingRecordId(List<Long> meetingRecordIds) {
        if (meetingRecordIds.isEmpty()) {
            return Map.of();
        }
        return jpaMeetingRecordMilestoneRepository.findAllByMeetingRecordIdIn(meetingRecordIds).stream()
            .collect(Collectors.groupingBy(
                MeetingRecordMilestoneJpaEntity::getMeetingRecordId,
                Collectors.mapping(MeetingRecordMilestoneJpaEntity::getMilestoneId, Collectors.toList())
            ));
    }

    private Map<Long, List<MeetingParticipant>> findParticipantsByMeetingRecordId(List<Long> meetingRecordIds) {
        if (meetingRecordIds.isEmpty()) {
            return Map.of();
        }
        return jpaMeetingParticipantRepository.findAllByMeetingRecordIdIn(meetingRecordIds).stream()
            .map(MeetingParticipantJpaEntity::toDomain)
            .collect(Collectors.groupingBy(MeetingParticipant::getMeetingRecordId));
    }
}
