package steam.vm.repo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import steam.vm.entity.InviteCodeEntity;

import java.util.Optional;

public interface InviteCodeRepository extends JpaRepository<InviteCodeEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InviteCodeEntity> findByCode(String code);
}
