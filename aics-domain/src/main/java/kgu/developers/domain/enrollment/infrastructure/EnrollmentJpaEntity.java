package kgu.developers.domain.enrollment.infrastructure;

import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "\"enrollment\"")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class EnrollmentJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sectionId;

    @Column(nullable = false, length = 32)
    private String studentNumber;

    @Column(nullable = false, length = 16)
    @Enumerated(STRING)
    private Role role;

    @Column(nullable = false, length = 16)
    @Enumerated(STRING)
    private Status status;

    public Enrollment toDomain() {
        return Enrollment.builder()
                .id(id)
                .sectionId(sectionId)
                .studentNumber(studentNumber)
                .role(role)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .deletedAt(getDeletedAt())
                .build();
    }

    public static EnrollmentJpaEntity toEntity(Enrollment enrollment) {
        EnrollmentJpaEntity entity = EnrollmentJpaEntity.builder()
                .id(enrollment.getId())
                .sectionId(enrollment.getSectionId())
                .studentNumber(enrollment.getStudentNumber())
                .role(enrollment.getRole())
                .status(enrollment.getStatus())
                .build();
        entity.createdAt = enrollment.getCreatedAt();
        entity.setDeletedAt(enrollment.getDeletedAt());
        return entity;
    }
}
