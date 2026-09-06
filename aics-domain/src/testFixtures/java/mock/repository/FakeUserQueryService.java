package mock.repository;

import java.util.ArrayList;
import java.util.List;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;

public class FakeUserQueryService extends UserQueryService {

    private final List<User> users = new ArrayList<>();

    public FakeUserQueryService() {
        super(null, null);
    }

    public void save(User user) {
        users.add(user);
    }

    @Override
    public User getUserByStudentNumber(String studentNumber) {
        return users.stream()
            .filter(user -> user.getStudentNumber().equals(studentNumber))
            .findFirst()
            .orElseThrow(() -> new kgu.developers.domain.user.exception.UserNotFoundException());
    }

    @Override
    public List<User> getUsersByStudentNumbers(List<String> studentNumbers) {
        return users.stream()
            .filter(user -> studentNumbers.contains(user.getStudentNumber()))
            .toList();
    }

    public void clear() {
        users.clear();
    }
}