package kgu.developers.domain.section.domain;

import java.util.List;
import java.util.Optional;

public interface SectionRepository {
    Section save(Section section);

    Optional<SectionDetail> findById(Long id);

    List<SectionDetail> findAllByCourseId(Long courseId);

    List<SectionDetail> findAllByProfessorId(String professorId);

    List<SectionDetail> findAllByIdIn(List<Long> ids);

    boolean existsActiveByIdAndProfessorId(Long id, String professorId);

    Optional<Section> findActiveByIdForUpdate(Long id);
}
