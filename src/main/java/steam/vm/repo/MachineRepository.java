package steam.vm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import steam.vm.entity.MachineEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MachineRepository extends JpaRepository<MachineEntity, Long> {
    List<MachineEntity> findAllByUser_IdOrderByNameAsc(Long userId);
    Optional<MachineEntity> findByUser_IdAndDeviceKey(Long userId, String deviceKey);
    Optional<MachineEntity> findByIdAndUser_Id(Long id, Long userId);
    List<MachineEntity> findAllByStatusAndLastSeenBefore(MachineEntity.Status status, LocalDateTime cutoff);
}


