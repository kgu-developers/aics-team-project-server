package kgu.developers.domain.auth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO refresh_token (student_number, token_hash, version)
        VALUES (:studentNumber, :tokenHash, 0)
        ON CONFLICT (student_number)
        DO UPDATE SET token_hash = EXCLUDED.token_hash, version = refresh_token.version + 1
        """, nativeQuery = true)
    void upsert(@Param("studentNumber") String studentNumber, @Param("tokenHash") String tokenHash);
}
