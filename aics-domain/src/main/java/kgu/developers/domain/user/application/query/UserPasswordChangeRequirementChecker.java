package kgu.developers.domain.user.application.query;

import org.springframework.stereotype.Component;

import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.globalutils.jwt.PasswordChangeRequirementChecker;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserPasswordChangeRequirementChecker implements PasswordChangeRequirementChecker {
    private final UserRepository userRepository;

    @Override
    public boolean isRequired(String studentNumber) {
        return userRepository.findByStudentNumber(studentNumber)
            .map(user -> user.isPasswordChangeRequired())
            .orElse(true);
    }
}
