package kgu.developers.domain.user.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findByStudentNumber(String student_number);

    List<User> findAllOrderByStudentNumber();
}
