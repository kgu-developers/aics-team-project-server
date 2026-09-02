package kgu.developers.domain.fileobject.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.fileobject.domain.FileObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_object")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class FileObjectJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "uploaded_by", nullable = false, length = 20)
    private String uploadedBy;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Column(name = "preview_supported", nullable = false)
    private boolean previewSupported;

    @Column(name = "preview_type", length = 32)
    private String previewType;

    public static FileObjectJpaEntity fromDomain(FileObject fileObject) {
        return FileObjectJpaEntity.builder()
                .id(fileObject.getId())
                .uploadedBy(fileObject.getUploadedBy())
                .storageKey(fileObject.getStorageKey())
                .fileName(fileObject.getFileName())
                .contentType(fileObject.getContentType())
                .size(fileObject.getSize())
                .previewSupported(fileObject.isPreviewSupported())
                .previewType(fileObject.getPreviewType())
                .build();
    }

    public FileObject toDomain() {
        return FileObject.builder()
                .id(id)
                .uploadedBy(uploadedBy)
                .storageKey(storageKey)
                .fileName(fileName)
                .contentType(contentType)
                .size(size)
                .previewSupported(previewSupported)
                .previewType(previewType)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .deletedAt(getDeletedAt())
                .build();
    }
}
