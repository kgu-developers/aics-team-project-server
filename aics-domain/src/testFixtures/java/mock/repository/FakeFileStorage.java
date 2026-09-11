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

    @Override
    public String presignedUrl(String storageKey) {
        return "https://fake-storage.local/" + storageKey;
    }

    @Override
    public InputStream download(String storageKey) {
        return new ByteArrayInputStream(("fake content: " + storageKey).getBytes(StandardCharsets.UTF_8));
    }
}
