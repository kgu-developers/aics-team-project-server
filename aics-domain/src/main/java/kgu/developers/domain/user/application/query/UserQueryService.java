package kgu.developers.domain.user.application.query;

import java.util.List;

import kgu.developers.domain.auth.domain.LoginRole;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.user.domain.User;
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

    public List<User> getUsersByStudentNumbers(List<String> studentNumbers) {
        return userRepository.findAllByStudentNumberIn(studentNumbers);
    }

    // 제출 이력처럼 "그 시점에 누가 했는지"를 보여줘야 하는 화면은, 그 뒤 탈퇴(소프트 삭제)한
    // 사용자라도 이름이 계속 보여야 한다 — findAllByStudentNumberIn은 deletedAt IS NULL만
    // 찾아서 탈퇴한 제출자의 이름이 조용히 사라지는 문제가 있었다.
    public List<User> getUsersByStudentNumbersIncludingDeleted(List<String> studentNumbers) {
        return userRepository.findAllIncludingDeletedByStudentNumberIn(studentNumbers);
    }

    public User getUserByStudentNumber(String studentNumber) {
        return userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(UserNotFoundException::new);
    }

    public LoginRole getUserRole(User user) {
        return switch (user.getGlobalRole()) {
            case ADMIN -> LoginRole.ADMIN;
            case USER -> {
                boolean assistant = enrollmentRepository.findAllByUserId(user.getStudentNumber()).stream()
                        .anyMatch(Enrollment::isActiveAssistant);
                yield assistant ? LoginRole.ASSISTANT : LoginRole.STUDENT;
            }
        };
    }

    public LoginRole getUserRoleByStudentNumber(String studentNumber) {
        return getUserRole(getUserByStudentNumber(studentNumber));
    }
}
