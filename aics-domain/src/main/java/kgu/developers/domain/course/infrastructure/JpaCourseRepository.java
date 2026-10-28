package kgu.developers.domain.course.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaCourseRepository extends JpaRepository<CourseJpaEntity, Long> {
	Optional<CourseJpaEntity> findByIdAndDeletedAtIsNull(Long id);

	// 강좌는 교수가 소유하지 않는 공용 카탈로그라 FK가 없다. 본인 분반이 하나라도 달린
	// 강좌는 당연히 보여야 하고, 분반이 아직 하나도 없는 강좌(막 생성됐거나 아무도 안 쓴
	// 강좌)는 누구든 첫 분반을 붙일 수 있어야 하므로 전원에게 보여야 한다. 그래서
	// "본인 분반이 있음" OR "분반이 아예 없음" 둘 중 하나면 반환한다.
	@Query("""
			select c from CourseJpaEntity c
			where c.deletedAt is null
			and (
				c.id in (
					select s.course.id from SectionJpaEntity s
					where s.professor.studentNumber = :professorId
					and s.deletedAt is null
				)
				or c.id not in (
					select s.course.id from SectionJpaEntity s
					where s.deletedAt is null
				)
			)
			order by c.year asc, c.name asc
			""")
	List<CourseJpaEntity> findAllOwnedByProfessor(@Param("professorId") String professorId);
}
