package mock.repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    @Override
    public InputStream download(String storageKey) {
        return new ByteArrayInputStream(("fake content: " + storageKey).getBytes(StandardCharsets.UTF_8));
    }
}
