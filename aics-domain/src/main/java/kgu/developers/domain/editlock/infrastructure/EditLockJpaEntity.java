package kgu.developers.domain.editlock.infrastructure;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.editlock.domain.EditLock;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"edit_lock\"", uniqueConstraints =
    @UniqueConstraint(name = "uk_edit_lock_target", columnNames = {"target_type", "target_id"}))
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class EditLockJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "target_type", nullable = false, length = 30)
    @Enumerated(STRING)
    private EditLockTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "locked_by", nullable = false, length = 20)
    private String lockedBy;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    public EditLock toDomain() {
        return EditLock.builder()
            .id(this.id)
            .targetType(this.targetType)
            .targetId(this.targetId)
            .lockedBy(this.lockedBy)
            .lockedAt(this.lockedAt)
            .build();
    }

    public static EditLockJpaEntity toEntity(EditLock domain) {
        return EditLockJpaEntity.builder()
            .id(domain.getId())
            .targetType(domain.getTargetType())
            .targetId(domain.getTargetId())
            .lockedBy(domain.getLockedBy())
            .lockedAt(domain.getLockedAt())
            .build();
    }
}
