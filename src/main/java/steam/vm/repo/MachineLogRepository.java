package steam.vm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import steam.vm.entity.MachineLogEntity;
import java.util.List;

public interface MachineLogRepository extends JpaRepository<MachineLogEntity, Long> {
    List<MachineLogEntity> findTop500ByMachine_User_IdOrderByCreatedAtDesc(Long userId);
    List<MachineLogEntity> findTop500ByMachine_IdAndMachine_User_IdOrderByCreatedAtDesc(Long machineId, Long userId);
    List<MachineLogEntity> findTop500ByMachine_User_IdAndSourceOrderByCreatedAtDesc(Long userId, MachineLogEntity.Source source);
    List<MachineLogEntity> findTop500ByMachine_IdAndMachine_User_IdAndSourceInOrderByCreatedAtDesc(
            Long machineId, Long userId, List<MachineLogEntity.Source> sources);
}
