package kgu.developers.domain.user.application.command;

import kgu.developers.domain.auth.infrastructure.JpaRefreshTokenRepository;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.DuplicateEmailException;
import kgu.developers.domain.user.exception.DuplicateStudentNumberException;
import kgu.developers.domain.user.exception.InvalidCredentialsException;
import kgu.developers.globalutils.jwt.TokenRevocationStore;
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
    private final JpaRefreshTokenRepository refreshTokenRepository;
    private final TokenRevocationStore tokenRevocationStore;

    public String createUser(String studentNumber, String email, String name, String password,
                             UserGlobalRole globalRole, String phone, boolean reactivate) {
        if (userRepository.existsByStudentNumber(studentNumber)) {
            User existing = userRepository.findIncludingDeleted(studentNumber)
                    .orElseThrow(DuplicateStudentNumberException::new);
            if (!reactivate || existing.getDeletedAt() == null) {
                throw new DuplicateStudentNumberException();
            }
            userRepository.archiveAndHardDelete(existing);
        }
        checkEmailAvailable(email, studentNumber);
        User user = User.create(studentNumber, email, name, passwordEncoder.encode(password), globalRole, phone);
        return userRepository.save(user).getStudentNumber();
    }

    public void updateUser(User user, String email, String name, String password, UserGlobalRole globalRole,
                           String phone) {
        checkEmailAvailable(email, user.getStudentNumber());
        boolean credentialsChanged = password != null || globalRole != user.getGlobalRole();

        user.updateEmail(email);
        user.updateName(name);
        if (password != null) {
            user.updatePassword(passwordEncoder.encode(password));
        }
        user.updateGlobalRole(globalRole);
        user.updatePhone(phone);
        userRepository.save(user);

        if (credentialsChanged) {
            revokeTokens(user);
        }
    }

    public void updatePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        revokeTokens(user);
    }

    public void deleteUser(User user) {
        user.delete();
        userRepository.save(user);
        revokeTokens(user);
    }

    private void revokeTokens(User user) {
        refreshTokenRepository.deleteById(user.getStudentNumber());
        tokenRevocationStore.revokeTokensIssuedBefore(user.getStudentNumber());
    }

    private void checkEmailAvailable(String email, String studentNumber) {
        if (userRepository.existsByEmailAndStudentNumberNotAndDeletedAtIsNull(email, studentNumber)) {
            throw new DuplicateEmailException();
        }
    }
}
