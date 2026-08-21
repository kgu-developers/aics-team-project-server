package kgu.developers.domain.user.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    boolean existsByStudentNumber(String studentNumber);

    boolean existsByEmailAndStudentNumberNotAndDeletedAtIsNull(String email, String studentNumber);

    Optional<User> findByStudentNumber(String studentNumber);

    Optional<User> findIncludingDeleted(String studentNumber);

    void archiveAndHardDelete(User user);

    List<User> findAllOrderByStudentNumber();

    List<User> findAllByStudentNumberIn(List<String> studentNumbers);

    List<User> findAllIncludingDeletedByStudentNumberIn(List<String> studentNumbers);

    List<User> findAllByEmailIn(List<String> emails);
}
