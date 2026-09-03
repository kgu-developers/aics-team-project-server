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

    /**
     * 이 분반 행을 PESSIMISTIC_WRITE로 잠근 채 조회한다. 반환값을 안 쓰고 존재확인+잠금
     * 용도로만 호출해도 되지만(TeamImportFacade/EnrollmentImportFacade가 이렇게 씀),
     * 잠금은 호출부의 트랜잭션이 끝날 때까지 유지되므로 이미 다른 이유로 이 섹션 행을
     * 잠그는 코드와 같은 트랜잭션에서 같이 호출하면 예상 못 한 락 경합/데드락이 날 수 있다.
     */
    Optional<Section> findActiveByIdForUpdate(Long id);

    boolean lockActiveByIdAndProfessorId(Long id, String professorId);
}
