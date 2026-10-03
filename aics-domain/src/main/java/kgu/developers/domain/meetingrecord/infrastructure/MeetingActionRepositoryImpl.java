package kgu.developers.domain.meetingrecord.infrastructure;

import java.util.List;
import java.util.Optional;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionRepository;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.exception.MeetingActionConcurrentlyModifiedException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<MeetingAction> findAllByTeamIdIn(
        List<Long> teamIds,
        Long meetingRecordId,
        MeetingActionStatus status,
        Pageable pageable
    ) {
        // 담당 분반에 팀이 하나도 없으면 in () 쿼리를 날리지 않고 빈 페이지로 끝낸다.
        if (teamIds == null || teamIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return jpaMeetingActionRepository.findAllByTeamIdIn(teamIds, meetingRecordId, status, pageable)
            .map(MeetingActionJpaEntity::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaMeetingActionRepository.deleteById(id);
    }
}
