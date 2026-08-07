package kgu.developers.domain.user.infrastructure;

import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final JpaUserRepository jpaUserRepository;

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.toEntity(user);
        UserJpaEntity savedEntity = jpaUserRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public boolean existsByStudentNumber(String studentNumber) {
        return jpaUserRepository.existsById(studentNumber);
    }

    @Override
    public boolean existsByEmailAndStudentNumberNot(String email, String studentNumber) {
        return jpaUserRepository.existsByEmailAndStudentNumberNot(email, studentNumber);
    }

    @Override
    public Optional<User> findByStudentNumber(String studentNumber) {
        Optional<UserJpaEntity> optionalEntity = jpaUserRepository.findByStudentNumberAndDeletedAtIsNull(studentNumber);
        return optionalEntity.map(UserJpaEntity::toDomain);
    }

    @Override
    public List<User> findAllOrderByStudentNumber() {
        List<UserJpaEntity> entities = jpaUserRepository.findAllByDeletedAtIsNullOrderByStudentNumberAsc();
        return entities.stream()
                .map(UserJpaEntity::toDomain)
                .collect(Collectors.toList());
    }
}
