package kgu.developers.domain.fileobject.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
class S3FileStorageTest {

    @Mock private S3Client s3Client;
    @Mock private S3Presigner s3Presigner;

    @Test
    void upload_usesProvidedContentTypeForS3Metadata() {
        S3FileStorage storage = new S3FileStorage(s3Client, s3Presigner);
        ReflectionTestUtils.setField(storage, "bucket", "bucket");
        MockMultipartFile file = new MockMultipartFile("file", "screen.png", "application/octet-stream", new byte[] {1});

        storage.upload(file, "image/png");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        then(s3Client).should().putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().contentType()).isEqualTo("image/png");
    }

    @Test
    void delete_removesObjectFromS3() {
        S3FileStorage storage = new S3FileStorage(s3Client, s3Presigner);
        ReflectionTestUtils.setField(storage, "bucket", "bucket");

        storage.delete("projects/screen.png");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        then(s3Client).should().deleteObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("bucket");
        assertThat(request.getValue().key()).isEqualTo("projects/screen.png");
    }

    @Test
    void upload_usesProvidedFileNameForS3Key() {
        S3FileStorage storage = new S3FileStorage(s3Client, s3Presigner);
        ReflectionTestUtils.setField(storage, "bucket", "bucket");
        MockMultipartFile file = new MockMultipartFile("file", "raw-name.png", "image/png", new byte[] {1});

        storage.upload(file, "image/png", "image");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        then(s3Client).should().putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().key()).endsWith("-image");
    }
}
