package mock.repository;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.multipart.MultipartFile;

import kgu.developers.domain.fileobject.domain.FileStorage;

public class FakeFileStorage implements FileStorage {

    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public String upload(MultipartFile file) {
        return "fake/" + sequence.incrementAndGet() + "-" + file.getOriginalFilename();
    }

    @Override
    public String presignedUrl(String storageKey) {
        return "https://fake-storage.local/" + storageKey;
    }
}
