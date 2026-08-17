package user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.user.infrastructure.JpaUserRepository;
import kgu.developers.domain.user.infrastructure.UserRepositoryImpl;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

  @Mock
  private JpaUserRepository jpaUserRepository;

  @InjectMocks
  private UserRepositoryImpl userRepository;

  @Test
  @DisplayName("existsByStudentNumber는 소프트삭제된 학번도 사용 중으로 본다")
  void existsByStudentNumberIncludesDeleted() {
    given(jpaUserRepository.existsById("202699999")).willReturn(true);

    assertThat(userRepository.existsByStudentNumber("202699999")).isTrue();
  }
}
