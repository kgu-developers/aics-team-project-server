package mock.repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.multipart.MultipartFile;

import kgu.developers.domain.fileobject.domain.FileStorage;

public class FakeFileStorage implements FileStorage {

    private final AtomicLong sequence = new AtomicLong(0);
    private final Set<String> storageKeys = ConcurrentHashMap.newKeySet();

    @Override
    public String upload(MultipartFile file) {
        return upload(file, file.getContentType(), file.getOriginalFilename());
    }

    @Override
    public String upload(MultipartFile file, String contentType) {
        return upload(file, contentType, file.getOriginalFilename());
    }

    @Override
    public String upload(MultipartFile file, String contentType, String fileName) {
        String storageKey = "fake/" + sequence.incrementAndGet() + "-" + fileName;
        storageKeys.add(storageKey);
        return storageKey;
    }

    @Override
    public void delete(String storageKey) {
        storageKeys.remove(storageKey);
    }

    /** 지금 저장소에 남아 있는 파일 키. 업로드 뒤 정리(삭제)가 됐는지 확인하는 테스트용이다. */
    public Set<String> storedKeys() {
        return Set.copyOf(storageKeys);
    }

    @Override
    public String presignedUrl(String storageKey) {
        return "https://fake-storage.local/" + storageKey;
    }

    @Override
    public InputStream download(String storageKey) {
        return new ByteArrayInputStream(("fake content: " + storageKey).getBytes(StandardCharsets.UTF_8));
    }
}
