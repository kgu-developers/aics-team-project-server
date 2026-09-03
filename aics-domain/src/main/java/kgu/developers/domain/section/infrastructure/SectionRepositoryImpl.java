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
    public Optional<Section> findActiveByIdForUpdate(Long id) {
        return jpaSectionRepository.findActiveByIdForUpdate(id).map(SectionJpaEntity::toDomain);
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

    @Override
    public List<SectionDetail> findAllByIdIn(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<SectionJpaEntity> entities = jpaSectionRepository.findAllByIdInAndDeletedAtIsNullOrderByCodeAsc(ids);
        return entities.stream()
                .map(SectionJpaEntity::toDetail)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsActiveByIdAndProfessorId(Long id, String professorId) {
        return jpaSectionRepository.findByIdAndDeletedAtIsNull(id)
                .filter(section -> isOwnedByProfessor(section, professorId))
                .isPresent();
    }

    @Override
    public boolean lockActiveByIdAndProfessorId(Long id, String professorId) {
        return jpaSectionRepository
                .findByIdAndProfessorStudentNumberAndDeletedAtIsNull(id, professorId)
                .isPresent();
    }

    private boolean isOwnedByProfessor(SectionJpaEntity section, String professorId) {
        return section != null
                && section.getDeletedAt() == null
                && section.getProfessor() != null
                && professorId != null
                && professorId.equals(section.getProfessor().getStudentNumber());
    }
}
