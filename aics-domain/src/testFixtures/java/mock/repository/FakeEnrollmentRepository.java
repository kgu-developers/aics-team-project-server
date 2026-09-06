package mock.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;

public class FakeEnrollmentRepository implements EnrollmentRepository {

    private final Map<Long, Enrollment> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Enrollment save(Enrollment enrollment) {
        Long id = enrollment.getId() != null ? enrollment.getId() : sequence.incrementAndGet();

        Enrollment saved = Enrollment.builder()
            .id(id)
            .sectionId(enrollment.getSectionId())
            .userId(enrollment.getUserId())
            .role(enrollment.getRole())
            .status(enrollment.getStatus())
            .createdAt(enrollment.getCreatedAt() != null ? enrollment.getCreatedAt() : LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .deletedAt(enrollment.getDeletedAt())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<Enrollment> findById(Long id) {
        return Optional.ofNullable(store.get(id))
            .filter(enrollment -> enrollment.getDeletedAt() == null);
    }

    @Override
    public boolean existsBySectionIdAndUserId(Long sectionId, String userId) {
        return findBySectionIdAndUserId(sectionId, userId).isPresent();
    }

    @Override
    public Optional<Enrollment> findBySectionIdAndUserId(Long sectionId, String userId) {
        return store.values().stream()
            .filter(enrollment -> enrollment.getDeletedAt() == null)
            .filter(enrollment -> enrollment.getSectionId().equals(sectionId))
            .filter(enrollment -> enrollment.getUserId().equals(userId))
            .findFirst();
    }

    @Override
    public Optional<Enrollment> findBySectionIdAndUserIdForUpdate(Long sectionId, String userId) {
        return findBySectionIdAndUserId(sectionId, userId);
    }

    @Override
    public Optional<Enrollment> findIncludingDeleted(Long sectionId, String userId) {
        return store.values().stream()
            .filter(enrollment -> enrollment.getSectionId().equals(sectionId))
            .filter(enrollment -> enrollment.getUserId().equals(userId))
            .findFirst();
    }

    @Override
    public List<Enrollment> findAllBySectionId(Long sectionId) {
        return store.values().stream()
            .filter(enrollment -> enrollment.getDeletedAt() == null)
            .filter(enrollment -> enrollment.getSectionId().equals(sectionId))
            .toList();
    }

    @Override
    public List<Enrollment> findAllByUserId(String userId) {
        return store.values().stream()
            .filter(enrollment -> enrollment.getDeletedAt() == null)
            .filter(enrollment -> enrollment.getUserId().equals(userId))
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        Optional.ofNullable(store.get(id)).ifPresent(Enrollment::delete);
    }
}
