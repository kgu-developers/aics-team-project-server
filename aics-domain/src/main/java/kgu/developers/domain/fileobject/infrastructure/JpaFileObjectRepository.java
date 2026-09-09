package kgu.developers.domain.fileobject.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaFileObjectRepository extends JpaRepository<FileObjectJpaEntity, Long> {
    Optional<FileObjectJpaEntity> findByIdAndDeletedAtIsNull(Long id);
    
    List<FileObjectJpaEntity> findAllByIdInAndDeletedAtIsNull(List<Long> ids);
}
