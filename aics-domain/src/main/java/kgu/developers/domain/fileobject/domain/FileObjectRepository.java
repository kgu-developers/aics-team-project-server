package kgu.developers.domain.fileobject.domain;

import java.util.List;
import java.util.Optional;

public interface FileObjectRepository {
    FileObject save(FileObject fileObject);

    Optional<FileObject> findById(Long id);

    List<FileObject> findAllById(List<Long> ids);

    List<FileObject> findAllByIdAndDeletedAtIsNull(List<Long> ids);
}
