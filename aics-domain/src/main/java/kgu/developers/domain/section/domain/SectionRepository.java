package kgu.developers.domain.section.domain;

import java.util.List;
import java.util.Optional;

public interface SectionRepository {
    Section save(Section section);

    Optional<Section> findById(Long id);

    List<SectionDetail> findAllByCourseId(Long courseId);

    List<SectionDetail> findAllByProfessorId(String professorId);
}
