package kgu.developers.domain.user.domain;

import lombok.*;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class User {
    private String studentNumber;
    private String email;
    private String name;
    private String password;

    private UserGlobalRole globalRole;
    private String phone;

    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    protected LocalDateTime deletedAt;

    public static User create(String studentNumber, String email, String name, String password,
                                 UserGlobalRole globalRole, String phone) {
        return User.builder()
                .studentNumber(studentNumber)
                .email(email)
                .name(name)
                .password(password)
                .globalRole(globalRole)
                .phone(phone)
                .build();
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateGlobalRole(UserGlobalRole globalRole) {
        this.globalRole = globalRole;
    }

    public void updatePhone(String phone) {
        this.phone = phone;
    }

    public void delete() {
        deletedAt = LocalDateTime.now();
    }
}
