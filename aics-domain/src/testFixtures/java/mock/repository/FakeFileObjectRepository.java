package mock.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
        return Optional.ofNullable(store.get(id))
            .filter(fileObject -> fileObject.getDeletedAt() == null);
    }

    @Override
    public List<FileObject> findAllById(List<Long> ids) {
        return ids.stream()
                .distinct()
                .map(store::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileObject> findAllByIdAndDeletedAtIsNull(List<Long> ids) {
        return ids.stream()
                .distinct()
                .map(store::get)
                .filter(java.util.Objects::nonNull)
                .filter(fileObject -> fileObject.getDeletedAt() == null)
                .collect(Collectors.toList());
    }
}
