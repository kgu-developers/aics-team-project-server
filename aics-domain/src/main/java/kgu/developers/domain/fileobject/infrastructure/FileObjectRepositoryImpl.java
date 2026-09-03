package kgu.developers.domain.fileobject.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FileObjectRepositoryImpl implements FileObjectRepository {
    private final JpaFileObjectRepository jpaFileObjectRepository;

    @Override
    public FileObject save(FileObject fileObject) {
        FileObjectJpaEntity entity = FileObjectJpaEntity.fromDomain(fileObject);
        return jpaFileObjectRepository.save(entity).toDomain();
    }

    @Override
    public Optional<FileObject> findById(Long id) {
        return jpaFileObjectRepository.findByIdAndDeletedAtIsNull(id)
                .map(FileObjectJpaEntity::toDomain);
    }
}
