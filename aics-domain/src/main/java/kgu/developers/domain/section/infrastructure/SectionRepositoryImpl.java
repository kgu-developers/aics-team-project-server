package kgu.developers.domain.section.infrastructure;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SectionRepositoryImpl implements SectionRepository {
    private final JpaSectionRepository jpaSectionRepository;
    private final EntityManager entityManager;

    @Override
    public Section save(Section section) {
        // 강좌/교수 존재 여부는 SectionCommandService가 검증한다. 여기서는 조회 없이 FK만 채운다.
        CourseJpaEntity course = entityManager.getReference(CourseJpaEntity.class, section.getCourseId());
        UserJpaEntity professor = entityManager.getReference(UserJpaEntity.class, section.getProfessorId());
        SectionJpaEntity entity = SectionJpaEntity.toEntity(section, course, professor);
        SectionJpaEntity savedEntity = jpaSectionRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<SectionDetail> findById(Long id) {
        Optional<SectionJpaEntity> optionalEntity = jpaSectionRepository.findByIdAndDeletedAtIsNull(id);
        return optionalEntity.map(SectionJpaEntity::toDetail);
    }

    @Override
    public List<SectionDetail> findAllByCourseId(Long courseId) {
        List<SectionJpaEntity> entities = jpaSectionRepository.findAllByCourseIdAndDeletedAtIsNullOrderByCodeAsc(courseId);
        return entities.stream()
                .map(SectionJpaEntity::toDetail)
                .collect(Collectors.toList());
    }

    @Override
    public List<SectionDetail> findAllByProfessorId(String professorId) {
        List<SectionJpaEntity> entities = jpaSectionRepository.findAllByProfessorStudentNumberAndDeletedAtIsNullOrderByCodeAsc(professorId);
        return entities.stream()
                .map(SectionJpaEntity::toDetail)
                .collect(Collectors.toList());
    }
}
