package user.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserQueryService userQueryService;

    @Test
    @DisplayName("getAllUsers는 학번순 사용자 목록을 조회한다")
    void getAllUsers() {
        List<User> users = List.of(user("202600001", "김학생"), user("202600002", "이학생"));
        given(userRepository.findAllOrderByStudentNumber()).willReturn(users);

        List<User> result = userQueryService.getAllUsers();

        assertThat(result).containsExactlyElementsOf(users);
    }

    @Test
    @DisplayName("getUsersByStudentNumbers는 학번 목록에 해당하는 사용자를 조회한다")
    void getUsersByStudentNumbers() {
        List<String> studentNumbers = List.of("202600001", "202600002");
        List<User> users = List.of(user("202600001", "김학생"), user("202600002", "이학생"));
        given(userRepository.findAllByStudentNumberIn(studentNumbers)).willReturn(users);

        List<User> result = userQueryService.getUsersByStudentNumbers(studentNumbers);

        assertThat(result).containsExactlyElementsOf(users);
        verify(userRepository).findAllByStudentNumberIn(studentNumbers);
    }

    @Test
    @DisplayName("getUserByStudentNumber는 학번에 해당하는 사용자를 조회한다")
    void getUserByStudentNumber() {
        User user = user("202600001", "김학생");
        given(userRepository.findByStudentNumber("202600001")).willReturn(Optional.of(user));

        User result = userQueryService.getUserByStudentNumber("202600001");

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("getUserByStudentNumber는 존재하지 않는 사용자면 예외를 던진다")
    void getUserByStudentNumberWithMissingUser() {
        given(userRepository.findByStudentNumber("202600001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.getUserByStudentNumber("202600001"))
                .isInstanceOf(UserNotFoundException.class);
    }

    private User user(String studentNumber, String name) {
        return User.create(studentNumber, studentNumber + "@kgu.ac.kr", name, "encoded",
                UserGlobalRole.USER, "010-0000-0000");
    }
}
