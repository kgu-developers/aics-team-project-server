package kgu.developers.domain.user.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

/**
 * 탈퇴 회원의 이력 보관용.
 * 같은 학번이 여러 번 탈퇴할 수 있으므로 학번을 PK로 쓰지 않는다.
 * 비밀번호 해시는 이력 추적에 필요 없어 옮기지 않는다.
 */
@Entity
@Table(
        name = "user_archive",
        // 같은 학번이 같은 시각에 두 번 탈퇴할 수는 없다. 동시 재활성화 요청이 이력을 중복 적재하는 걸 DB에서 막는다.
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_number", "deleted_at"})
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class UserArchiveJpaEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String studentNumber;

    @Column(nullable = false, length = 64)
    private String email;

    @Column(nullable = false, length = 32)
    private String name;

    @Enumerated(STRING)
    @Column(nullable = false, length = 16)
    private UserGlobalRole globalRole;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime archivedAt;

    private UserArchiveJpaEntity(User user) {
        this.studentNumber = user.getStudentNumber();
        this.email = user.getEmail();
        this.name = user.getName();
        this.globalRole = user.getGlobalRole();
        this.phone = user.getPhone();
        this.createdAt = user.getCreatedAt();
        this.deletedAt = user.getDeletedAt();
    }

    public static UserArchiveJpaEntity from(User user) {
        return new UserArchiveJpaEntity(user);
    }
}
