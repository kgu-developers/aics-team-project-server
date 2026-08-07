package kgu.developers.domain.user.application.command;

import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.DuplicateStudentNumberException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {
    private final UserRepository UserRepository;
    private final PasswordEncoder passwordEncoder;

    public String createUser(String student_number, String email, String name, String password,
                             UserGlobalRole global_role, String phone) {
        if (UserRepository.existsByStudentNumber(student_number)) {
            throw new DuplicateStudentNumberException();
        }
        User user = User.create(student_number, email, name, passwordEncoder.encode(password), global_role, phone);
        return UserRepository.save(user).getStudent_number();
    }

    public void updateUser(User user, String name, String password, UserGlobalRole global_role, String phone) {
        user.updateName(name);
        user.updatePassword(passwordEncoder.encode(password));
        user.updateGlobalRole(global_role);
        user.updatePhone(phone);
        UserRepository.save(user);
    }

    public void updatePassword(User user, String password) {
        user.updatePassword(passwordEncoder.encode(password));
        UserRepository.save(user);
    }

    public void deleteUser(User user) {
        user.delete();
        UserRepository.save(user);
    }
}
