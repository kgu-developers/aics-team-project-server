package kgu.developers.domain.project.infrastructure;

import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.project.domain.ProposalSection;
import kgu.developers.domain.project.domain.ProposalSectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "proposal_section", uniqueConstraints = @UniqueConstraint(name = "uk_proposal_section_project_type", columnNames = {
    "project_id", "type" }))
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ProposalSectionJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    // 제안서와 생명주기가 같다. 제안서가 지워지거나 되살아나면 섹션도 같이 지운다(하드 삭제).
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private ProposalSectionType type;

    @Column(length = 20)
    private String assigneeUserId;

    @Column
    private LocalDateTime completedAt;

    public ProposalSection toDomain() {
        return ProposalSection.builder()
            .id(id)
            .projectId(projectId)
            .type(type)
            .assigneeUserId(assigneeUserId)
            .completedAt(completedAt)
            .createdAt(getCreatedAt())
            .updatedAt(getUpdatedAt())
            .build();
    }

    public static ProposalSectionJpaEntity toEntity(ProposalSection proposalSection) {
        ProposalSectionJpaEntity entity = ProposalSectionJpaEntity.builder()
            .id(proposalSection.getId())
            .projectId(proposalSection.getProjectId())
            .type(proposalSection.getType())
            .assigneeUserId(proposalSection.getAssigneeUserId())
            .completedAt(proposalSection.getCompletedAt())
            .build();
        if (proposalSection.getId() != null) {
            entity.createdAt = proposalSection.getCreatedAt();
        }
        return entity;
    }
}
