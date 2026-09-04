package kgu.developers.domain.editlock.application.query;

import java.time.LocalDateTime;
import java.util.Optional;
import kgu.developers.domain.editlock.domain.EditLock;
import kgu.developers.domain.editlock.domain.EditLockRepository;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EditLockQueryService {

    private final EditLockRepository editLockRepository;

    // 만료된 잠금은 행이 남아있어도 잠긴 것으로 보지 않는다(별도 정리 배치 없음, 조회 시점 계산).
    public Optional<EditLock> getActiveLock(EditLockTargetType targetType, Long targetId) {
        return editLockRepository.findByTargetTypeAndTargetId(targetType, targetId)
            .filter(editLock -> !editLock.isExpired(LocalDateTime.now()));
    }
}
