package kgu.developers.domain.team.infrastructure;

import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "team")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TeamJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_section_team"))
    private SectionJpaEntity section;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String kickoffRule;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String meetingSchedule;

    @Enumerated(STRING)
    @Column(nullable = false)
    private Status status;

    public Team toDomain() {
        return Team.builder()
                .id(id)
                .version(version)
                .sectionId(section.getId())
                .name(name)
                .kickoffRule(kickoffRule)
                .meetingSchedule(meetingSchedule)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .deletedAt(getDeletedAt())
                .build();
    }

    public static TeamJpaEntity toEntity(Team team, SectionJpaEntity section) {
        TeamJpaEntity  entity = TeamJpaEntity.builder()
                .id(team.getId())
                .version(team.getVersion())
                .section(section)
                .name(team.getName())
                .kickoffRule(team.getKickoffRule())
                .meetingSchedule(team.getMeetingSchedule())
                .status(team.getStatus())
                .build();
        entity.createdAt = team.getCreatedAt();
        entity.setDeletedAt(team.getDeletedAt());
        return entity;
    }
}
