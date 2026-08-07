package kgu.developers.domain.meetingrecord.infrastructure;

import java.util.List;
import java.util.Optional;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MeetingActionRepositoryImpl implements MeetingActionRepository {

    private final JpaMeetingActionRepository jpaMeetingActionRepository;

    @Override
    public MeetingAction save(MeetingAction meetingAction) {
        return jpaMeetingActionRepository.save(MeetingActionJpaEntity.toEntity(meetingAction)).toDomain();
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
    public void deleteById(Long id) {
        jpaMeetingActionRepository.deleteById(id);
    }
}
