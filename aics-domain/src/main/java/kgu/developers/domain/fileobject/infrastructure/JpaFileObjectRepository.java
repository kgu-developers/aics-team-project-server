package kgu.developers.domain.fileobject.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaFileObjectRepository extends JpaRepository<FileObjectJpaEntity, Long> {
}
