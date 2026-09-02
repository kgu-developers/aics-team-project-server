package mock.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;

public class FakeSectionRepository implements SectionRepository {

    private final Map<Long, Section> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Section save(Section section) {
        Long id = section.getId() != null ? section.getId() : sequence.incrementAndGet();

        Section saved = Section.builder()
            .id(id)
            .professorId(section.getProfessorId())
            .courseId(section.getCourseId())
            .code(section.getCode())
            .name(section.getName())
            .classTime(section.getClassTime())
            .capacity(section.getCapacity())
            .contactVisibleFrom(section.getContactVisibleFrom())
            .contactVisibleUntil(section.getContactVisibleUntil())
            .createdAt(section.getCreatedAt())
            .updatedAt(section.getUpdatedAt())
            .deletedAt(section.getDeletedAt())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<SectionDetail> findById(Long id) {
        return Optional.ofNullable(store.get(id))
            .map(section -> new SectionDetail(section, null, null));
    }

    @Override
    public List<SectionDetail> findAllByCourseId(Long courseId) {
        return store.values().stream()
            .filter(section -> section.getCourseId().equals(courseId))
            .map(section -> new SectionDetail(section, null, null))
            .toList();
    }

    @Override
    public List<SectionDetail> findAllByProfessorId(String professorId) {
        return store.values().stream()
            .filter(section -> section.getProfessorId().equals(professorId))
            .map(section -> new SectionDetail(section, null, null))
            .toList();
    }

    @Override
    public List<SectionDetail> findAllByIdIn(List<Long> ids) {
        // 실제 구현(findAllByIdInAndDeletedAtIsNullOrderByCodeAsc)과 같이 삭제분을 빼고 code 순으로 준다.
        return ids.stream()
            .map(store::get)
            .filter(Objects::nonNull)
            .filter(section -> section.getDeletedAt() == null)
            .sorted(Comparator.comparing(Section::getCode))
            .map(section -> new SectionDetail(section, null, null))
            .toList();
    }

    @Override
    public boolean existsActiveByIdAndProfessorId(Long id, String professorId) {
        return Optional.ofNullable(store.get(id))
            .filter(section -> section.getDeletedAt() == null)
            .filter(section -> section.getProfessorId().equals(professorId))
            .isPresent();
    }

    @Override
    public boolean lockActiveByIdAndProfessorId(Long id, String professorId) {
        return existsActiveByIdAndProfessorId(id, professorId);
    }
}
