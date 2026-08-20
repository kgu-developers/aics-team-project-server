package kgu.developers.domain.meetingrecord.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaMeetingRecordRepository extends JpaRepository<MeetingRecordJpaEntity, Long> {

    List<MeetingRecordJpaEntity> findAllByTeamId(Long teamId);

    List<MeetingRecordJpaEntity> findAllByTeamIdAndPhase(Long teamId, MeetingPhase phase);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MeetingRecordJpaEntity m where m.id = :id")
    Optional<MeetingRecordJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
