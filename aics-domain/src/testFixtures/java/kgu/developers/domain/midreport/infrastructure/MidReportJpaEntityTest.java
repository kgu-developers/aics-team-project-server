package kgu.developers.domain.midreport.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Version;
import java.time.LocalDateTime;
import kgu.developers.domain.midreport.domain.MidReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MidReportJpaEntityTest {
    @Test
    @DisplayName("중간보고서 JPA 엔티티는 문서 단위 낙관적 버전을 사용한다")
    void usesOptimisticVersion() throws Exception {
        assertThat(MidReportJpaEntity.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    @Test
    @DisplayName("중간보고서와 네 영역은 JPA 엔티티 왕복 시 보존된다")
    void mapsAggregate() {
        MidReport report = MidReport.create(
            1L,
            2L,
            "CineFlow 중간보고서",
            LocalDateTime.of(2026, 10, 26, 23, 59),
            "CineFlow",
            "영화관 관리"
        );

        MidReport restored = MidReportJpaEntity.fromDomain(report).toDomain();

        assertThat(restored.getTeamId()).isEqualTo(1L);
        assertThat(restored.getMilestoneId()).isEqualTo(2L);
        assertThat(restored.getBlocks()).hasSize(4);
        assertThat(restored.getBlocks().get(0).getFields().get(0).path("value").asText()).isEqualTo("CineFlow");
    }
}
