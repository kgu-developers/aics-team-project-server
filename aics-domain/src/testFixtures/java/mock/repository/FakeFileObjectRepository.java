package mock.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;

public class FakeFileObjectRepository implements FileObjectRepository {

    private final Map<Long, FileObject> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public FileObject save(FileObject fileObject) {
        Long id = fileObject.getId() != null ? fileObject.getId() : sequence.incrementAndGet();

        FileObject saved = FileObject.builder()
            .id(id)
            .uploadedBy(fileObject.getUploadedBy())
            .storageKey(fileObject.getStorageKey())
            .fileName(fileObject.getFileName())
            .contentType(fileObject.getContentType())
            .size(fileObject.getSize())
            .previewSupported(fileObject.isPreviewSupported())
            .previewType(fileObject.getPreviewType())
            .createdAt(fileObject.getCreatedAt())
            .updatedAt(fileObject.getUpdatedAt())
            .deletedAt(fileObject.getDeletedAt())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<FileObject> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }
}
