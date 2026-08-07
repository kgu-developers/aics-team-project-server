package kgu.developers.domain.user.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    boolean existsByStudentNumber(String studentNumber);

    Optional<User> findByStudentNumber(String studentNumber);

    List<User> findAllOrderByStudentNumber();
}
