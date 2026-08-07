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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String createUser(String studentNumber, String email, String name, String password,
                             UserGlobalRole globalRole, String phone) {
        if (userRepository.existsByStudentNumber(studentNumber)) {
            throw new DuplicateStudentNumberException();
        }
        User user = User.create(studentNumber, email, name, passwordEncoder.encode(password), globalRole, phone);
        return userRepository.save(user).getStudentNumber();
    }

    public void updateUser(User user, String name, String password, UserGlobalRole globalRole, String phone) {
        user.updateName(name);
        user.updatePassword(passwordEncoder.encode(password));
        user.updateGlobalRole(globalRole);
        user.updatePhone(phone);
        userRepository.save(user);
    }

    public void updatePassword(User user, String password) {
        user.updatePassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    public void deleteUser(User user) {
        user.delete();
        userRepository.save(user);
    }
}
