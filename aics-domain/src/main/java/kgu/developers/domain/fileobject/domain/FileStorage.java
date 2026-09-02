package kgu.developers.domain.fileobject.domain;

import org.springframework.web.multipart.MultipartFile;

// 실제 파일 바이트를 어디에 저장하는지를 감추는 포트. 구현체(S3FileStorage 등)만 바뀌어도
// 이 인터페이스를 쓰는 도메인 코드는 안 흔들리게 하려고 리포지토리와 별도로 분리했다.
public interface FileStorage {
    String upload(MultipartFile file);

    String presignedUrl(String storageKey);
}
