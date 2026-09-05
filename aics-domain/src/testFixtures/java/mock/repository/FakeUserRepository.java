package mock.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserRepository;

public class FakeUserRepository implements UserRepository {

    private final Map<String, User> store = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        store.put(user.getStudentNumber(), user);
        return user;
    }

    @Override
    public boolean existsByStudentNumber(String studentNumber) {
        return store.containsKey(studentNumber);
    }

    @Override
    public boolean existsByEmailAndStudentNumberNotAndDeletedAtIsNull(String email, String studentNumber) {
        return store.values().stream()
            .filter(user -> user.getDeletedAt() == null)
            .anyMatch(user -> user.getEmail().equals(email) && !user.getStudentNumber().equals(studentNumber));
    }

    @Override
    public Optional<User> findByStudentNumber(String studentNumber) {
        return Optional.ofNullable(store.get(studentNumber))
            .filter(user -> user.getDeletedAt() == null);
    }

    @Override
    public Optional<User> findIncludingDeleted(String studentNumber) {
        return Optional.ofNullable(store.get(studentNumber));
    }

    @Override
    public void archiveAndHardDelete(User user) {
        store.remove(user.getStudentNumber());
    }

    @Override
    public List<User> findAllOrderByStudentNumber() {
        return store.values().stream()
            .sorted(Comparator.comparing(User::getStudentNumber))
            .toList();
    }

    @Override
    public List<User> findAllByStudentNumberIn(List<String> studentNumbers) {
        return store.values().stream()
            .filter(user -> user.getDeletedAt() == null)
            .filter(user -> studentNumbers.contains(user.getStudentNumber()))
            .toList();
    }

    @Override
    public List<User> findAllIncludingDeletedByStudentNumberIn(List<String> studentNumbers) {
        return store.values().stream()
            .filter(user -> studentNumbers.contains(user.getStudentNumber()))
            .toList();
    }

    @Override
    public List<User> findAllByEmailIn(List<String> emails) {
        return store.values().stream()
            .filter(user -> user.getDeletedAt() == null)
            .filter(user -> emails.contains(user.getEmail()))
            .toList();
    }

    @Override
    public List<User> findAllIncludingDeletedByEmailIn(List<String> emails) {
        return store.values().stream()
            .filter(user -> emails.contains(user.getEmail()))
            .toList();
    }
}
