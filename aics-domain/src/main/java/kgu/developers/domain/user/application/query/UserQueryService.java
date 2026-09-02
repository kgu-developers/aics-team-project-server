package kgu.developers.domain.user.application.query;

import java.util.List;

import kgu.developers.domain.auth.domain.LoginRole;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    public List<User> getAllUsers() {
        return userRepository.findAllOrderByStudentNumber();
    }

    public User getUserByStudentNumber(String studentNumber) {
        return userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(UserNotFoundException::new);
    }

    public LoginRole getUserRoleByStudentNumber(String studentNumber) {
        UserGlobalRole globalRole = getUserByStudentNumber(studentNumber).getGlobalRole();
        if (globalRole != UserGlobalRole.USER) {
            return LoginRole.valueOf(globalRole.name());
        }

        boolean assistant = enrollmentRepository.findAllByUserId(studentNumber).stream()
                .filter(enrollment -> enrollment.getStatus() == Status.ACTIVE)
                .map(Enrollment::getRole)
                .anyMatch(role -> role == Role.ASSISTANT);
        return assistant ? LoginRole.ASSISTANT : LoginRole.STUDENT;
    }
}
