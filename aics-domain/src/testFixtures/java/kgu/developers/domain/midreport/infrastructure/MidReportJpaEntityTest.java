package kgu.developers.domain.midreport.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Version;
import java.time.LocalDateTime;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportBlockDefinition;
import kgu.developers.domain.midreport.domain.MidReportRevision;
import kgu.developers.domain.midreport.domain.MidReportStatus;
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

    @Test
    @DisplayName("영역 내용만 변경해도 부모 문서를 수정 상태로 만든다")
    void marksParentDirtyWhenBlockChanges() {
        MidReport report = MidReport.create(
            1L,
            2L,
            "CineFlow 중간보고서",
            LocalDateTime.of(2026, 10, 26, 23, 59),
            "CineFlow",
            "영화관 관리"
        );
        MidReportJpaEntity entity = MidReportJpaEntity.fromDomain(report);

        report.updateBlock(
            "topic",
            0L,
            MidReportBlockDefinition.TOPIC.emptyFields(java.util.Map.of(
                "title", "수정된 제목",
                "description", "수정된 설명"
            )),
            "202600001",
            LocalDateTime.of(2026, 9, 9, 0, 30)
        );
        entity.updateFromDomain(report);

        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("피드백 완료 메타데이터는 revision JSON 왕복 시 보존된다")
    void mapsRevisionCompletionMetadata() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 9, 9, 10, 0);
        LocalDateTime completedAt = LocalDateTime.of(2026, 9, 9, 12, 0);
        MidReport report = MidReport.builder()
            .teamId(1L)
            .milestoneId(2L)
            .title("보고서")
            .dueDate(LocalDateTime.of(2026, 10, 26, 23, 59))
            .status(MidReportStatus.SUBMITTED)
            .revision(new MidReportRevision(
                java.util.List.of("topic"), java.util.List.of("topic"), requestedAt, null,
                completedAt, "professor-1"
            ))
            .blocks(java.util.List.of())
            .build();

        MidReport restored = MidReportJpaEntity.fromDomain(report).toDomain();

        assertThat(restored.getRevision().completedAt()).isEqualTo(completedAt);
        assertThat(restored.getRevision().completedBy()).isEqualTo("professor-1");
        assertThat(restored.getRevision().resubmittedAt()).isNull();
    }

    @Test
    @DisplayName("기존 revision JSON은 완료 필드가 없거나 null이어도 읽을 수 있다")
    void mapsLegacyRevisionJsonWithoutCompletionMetadata() {
        MidReportJpaEntity entity = MidReportJpaEntity.builder()
            .teamId(1L)
            .milestoneId(2L)
            .title("보고서")
            .dueDate(LocalDateTime.of(2026, 10, 26, 23, 59))
            .status(MidReportStatus.SUBMITTED)
            .revisionData("""
                {"affectedBlockKeys":["topic"],"changedBlockKeys":[],
                 "requestedAt":"2026-09-09T10:00:00","resubmittedAt":null,"completedAt":null}
                """)
            .build();

        MidReport restored = entity.toDomain();

        assertThat(restored.getRevision().requestedAt()).isEqualTo(LocalDateTime.of(2026, 9, 9, 10, 0));
        assertThat(restored.getRevision().resubmittedAt()).isNull();
        assertThat(restored.getRevision().completedAt()).isNull();
        assertThat(restored.getRevision().completedBy()).isNull();
    }
}
