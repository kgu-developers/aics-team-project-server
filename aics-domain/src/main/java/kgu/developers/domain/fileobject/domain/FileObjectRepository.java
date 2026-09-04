package kgu.developers.domain.fileobject.domain;

import java.util.Optional;

public interface FileObjectRepository {
    FileObject save(FileObject fileObject);

    Optional<FileObject> findById(Long id);
}
