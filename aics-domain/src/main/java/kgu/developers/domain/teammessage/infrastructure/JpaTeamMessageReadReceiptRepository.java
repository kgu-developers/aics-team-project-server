package kgu.developers.domain.teammessage.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTeamMessageReadReceiptRepository extends JpaRepository<TeamMessageReadReceiptJpaEntity, Long> {

    List<TeamMessageReadReceiptJpaEntity> findAllByUserIdAndMessageIdIn(String userId, List<Long> messageIds);
}
