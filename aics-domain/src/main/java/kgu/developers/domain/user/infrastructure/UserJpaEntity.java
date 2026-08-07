package kgu.developers.domain.user.infrastructure;

import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "\"user\"")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class UserJpaEntity extends BaseTimeEntity {
    @Id
    @Column(length = 16)
    private String studentNumber;

    @Column(nullable = false, unique = true, length = 64)
    private String email;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(nullable = false)
    private String password;

    @Enumerated(STRING)
    @Column(nullable = false, length = 16)
    private UserGlobalRole globalRole;

    @Column(nullable = false, length = 20)
    private String phone;

    public User toDomain() {
        return User.builder()
                .student_number(studentNumber)
                .email(email)
                .name(name)
                .password(password)
                .global_role(globalRole)
                .phone(phone)
                .created_at(getCreatedAt())
                .updated_at(getUpdatedAt())
                .deleted_at(getDeletedAt())
                .build();
    }

    public static UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = UserJpaEntity.builder()
                .studentNumber(user.getStudent_number())
                .email(user.getEmail())
                .name(user.getName())
                .password(user.getPassword())
                .globalRole(user.getGlobal_role())
                .phone(user.getPhone())
                .build();
        entity.setDeletedAt(user.getDeleted_at());
        return entity;
    }
}
