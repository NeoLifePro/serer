package steam.vm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import steam.vm.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);

    default Optional<UserEntity> findReadableByUsername(String username) {
        return findByUsername(username);
    }

    default Optional<UserEntity> findReadableByEmail(String email) {
        return findByEmail(email);
    }

    default Optional<UserEntity> findReadableByUsernameOrEmail(String login) {
        return findReadableByUsername(login).or(() -> findReadableByEmail(login));
    }
}
