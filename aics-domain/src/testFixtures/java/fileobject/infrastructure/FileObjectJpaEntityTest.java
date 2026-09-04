package fileobject.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.infrastructure.FileObjectJpaEntity;

class FileObjectJpaEntityTest {

    @Test
    @DisplayName("fromDomain은 소프트삭제 시각을 그대로 옮긴다")
    void fromDomainKeepsDeletedAt() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 1, 9, 0);
        FileObject fileObject = FileObject.builder()
                .id(1L)
                .uploadedBy("202412345")
                .storageKey("key")
                .fileName("file.png")
                .contentType("image/png")
                .size(1024L)
                .previewSupported(false)
                .deletedAt(deletedAt)
                .build();

        FileObjectJpaEntity entity = FileObjectJpaEntity.fromDomain(fileObject);

        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }
}
