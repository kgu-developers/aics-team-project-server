package kgu.developers.domain.announcementpopup.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface JpaStudentAnnouncementPopupRepository extends Repository<AnnouncementPopupJpaEntity, Long> {
    @Query("""
        SELECT p FROM AnnouncementPopupJpaEntity p
        JOIN SectionAnnouncementJpaEntity a ON a.id = p.announcementId AND a.sectionId = p.sectionId
        JOIN SectionJpaEntity s ON s.id = p.sectionId
        JOIN s.course c
        JOIN EnrollmentJpaEntity e ON e.sectionId = s.id
        WHERE e.userId = :userId
          AND e.role = kgu.developers.domain.enrollment.domain.Role.STUDENT
          AND e.status = kgu.developers.domain.enrollment.domain.Status.ACTIVE
          AND e.deletedAt IS NULL AND s.deletedAt IS NULL AND c.deletedAt IS NULL
          AND c.status = kgu.developers.domain.course.domain.StatusType.ACTIVE
          AND p.enabled = true AND p.deletedAt IS NULL AND a.deletedAt IS NULL
          AND a.publishedAt <= :now
          AND (:popupId IS NULL OR p.id = :popupId)
        ORDER BY p.id DESC
        """)
    List<AnnouncementPopupJpaEntity> findAvailable(
        @Param("userId") String userId, @Param("popupId") Long popupId, @Param("now") LocalDateTime now);
}
