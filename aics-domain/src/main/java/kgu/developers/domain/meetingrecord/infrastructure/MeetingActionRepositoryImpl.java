package kgu.developers.domain.meetingrecord.infrastructure;

import java.util.List;
import java.util.Optional;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionRepository;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.exception.MeetingActionConcurrentlyModifiedException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MeetingActionRepositoryImpl implements MeetingActionRepository {

    private final JpaMeetingActionRepository jpaMeetingActionRepository;

    @Override
    public MeetingAction save(MeetingAction meetingAction) {
        try {
            return jpaMeetingActionRepository.saveAndFlush(MeetingActionJpaEntity.toEntity(meetingAction)).toDomain();
        } catch (OptimisticLockingFailureException e) {
            throw new MeetingActionConcurrentlyModifiedException();
        }
    }

    @Override
    public Optional<MeetingAction> findById(Long id) {
        return jpaMeetingActionRepository.findById(id).map(MeetingActionJpaEntity::toDomain);
    }

    @Override
    public List<MeetingAction> findAllByMeetingRecordId(Long meetingRecordId) {
        return jpaMeetingActionRepository.findAllByMeetingRecordId(meetingRecordId).stream()
            .map(MeetingActionJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<MeetingAction> findAllByTeamId(Long teamId, MeetingActionStatus status) {
        return jpaMeetingActionRepository.findAllByTeamId(teamId, status).stream()
                .map(MeetingActionJpaEntity::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaMeetingActionRepository.deleteById(id);
    }
}
