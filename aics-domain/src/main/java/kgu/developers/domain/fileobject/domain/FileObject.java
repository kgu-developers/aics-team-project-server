package kgu.developers.domain.fileobject.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class FileObject {
    private Long id;
    private String uploadedBy;
    private String storageKey;
    private String fileName;
    private String contentType;
    private long size;
    private boolean previewSupported;
    private String previewType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static FileObject create(
            String uploadedBy,
            String storageKey,
            String fileName,
            String contentType,
            long size,
            boolean previewSupported,
            String previewType
    ) {
        return FileObject.builder()
                .uploadedBy(uploadedBy)
                .storageKey(storageKey)
                .fileName(fileName)
                .contentType(contentType)
                .size(size)
                .previewSupported(previewSupported)
                .previewType(previewType)
                .build();
    }
}
