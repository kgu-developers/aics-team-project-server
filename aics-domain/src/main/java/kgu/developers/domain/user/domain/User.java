package kgu.developers.domain.user.domain;

import lombok.*;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class User {
    private String student_number;
    private String email;
    private String name;
    private String password;

    private UserGlobalRole global_role;
    private String phone;

    protected LocalDateTime created_at;
    protected LocalDateTime updated_at;
    protected LocalDateTime deleted_at;

    public static User create(String student_number, String email, String name, String password,
                                 UserGlobalRole global_role, String phone) {
        return User.builder()
                .student_number(student_number)
                .email(email)
                .name(name)
                .password(password)
                .global_role(global_role)
                .phone(phone)
                .build();
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateGlobalRole(UserGlobalRole global_role) {
        this.global_role = global_role;
    }

    public void updatePhone(String phone) {
        this.phone = phone;
    }

    public void delete() {
        deleted_at = LocalDateTime.now();
    }
}
