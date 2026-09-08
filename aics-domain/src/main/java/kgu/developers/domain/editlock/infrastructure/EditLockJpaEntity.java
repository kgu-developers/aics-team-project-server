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
import jakarta.persistence.Version;
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
    @UniqueConstraint(name = "uk_edit_lock_target", columnNames = {"target_type", "target_id", "section_key"}))
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

    // 대상 하나를 여러 사람이 섹션별로 동시에 편집할 수 있게 하는 구분자(KD3-213).
    // "섹션 없는 잠금"을 NULL로 표현하면 유니크 제약에서 NULL끼리는 서로 다른 값 취급돼
    // 동시성 보장이 깨지므로, 섹션이 없는 대상도 고정 문자열(예: "DEFAULT")을 쓰도록 NOT NULL로 강제한다.
    @Column(name = "section_key", nullable = false, length = 50)
    private String sectionKey;

    @Column(name = "locked_by", nullable = false, length = 20)
    private String lockedBy;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    @Version
    private Long version;

    public EditLock toDomain() {
        return EditLock.builder()
            .id(this.id)
            .targetType(this.targetType)
            .targetId(this.targetId)
            .sectionKey(this.sectionKey)
            .lockedBy(this.lockedBy)
            .lockedAt(this.lockedAt)
            .version(this.version)
            .build();
    }

    public static EditLockJpaEntity toEntity(EditLock domain) {
        return EditLockJpaEntity.builder()
            .id(domain.getId())
            .targetType(domain.getTargetType())
            .targetId(domain.getTargetId())
            .sectionKey(domain.getSectionKey())
            .lockedBy(domain.getLockedBy())
            .lockedAt(domain.getLockedAt())
            .version(domain.getVersion())
            .build();
    }
}
