package steam.vm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import steam.vm.entity.VmPanelEntity;

import java.util.List;
import java.util.Optional;

public interface VmPanelRepository extends JpaRepository<VmPanelEntity, Long> {
    Optional<VmPanelEntity> findByProfile_ProfileId(Long profileId);
    List<VmPanelEntity> findAllByProfile_User_Id(Long userId);

    @Modifying
    @Transactional
    @Query("""
        delete from VmPanelEntity v
        where v.profile.user.id = :userId
          and lower(v.vmName) = lower(:vmName)
    """)
    int deleteAllByUserIdAndVmName(@Param("userId") Long userId, @Param("vmName") String vmName);
}
