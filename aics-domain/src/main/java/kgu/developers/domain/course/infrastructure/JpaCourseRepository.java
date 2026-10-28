package kgu.developers.domain.course.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaCourseRepository extends JpaRepository<CourseJpaEntity, Long> {
	Optional<CourseJpaEntity> findByIdAndDeletedAtIsNull(Long id);

	// 강좌는 교수가 소유하지 않는 공용 카탈로그라 FK가 없다. 본인 분반이 하나라도 달린
	// 강좌만 추려야 해서, 분반 테이블(professor)까지 서브쿼리로 거쳐 조회한다.
	@Query("""
			select c from CourseJpaEntity c
			where c.deletedAt is null
			and c.id in (
				select s.course.id from SectionJpaEntity s
				where s.professor.studentNumber = :professorId
				and s.deletedAt is null
			)
			order by c.year asc, c.name asc
			""")
	List<CourseJpaEntity> findAllOwnedByProfessor(@Param("professorId") String professorId);
}
