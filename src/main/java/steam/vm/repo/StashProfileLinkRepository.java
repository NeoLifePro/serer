package steam.vm.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import steam.vm.entity.StashProfileLinkEntity;
import java.util.*;
public interface StashProfileLinkRepository extends JpaRepository<StashProfileLinkEntity,Long>{
 @EntityGraph(attributePaths={"farmProfile","farmProfile.vms"})
 List<StashProfileLinkEntity> findAllByStashProfile_ProfileId(Long id);
 long countByStashProfile_ProfileId(Long id);
 @EntityGraph(attributePaths={"stashProfile","farmProfile","farmProfile.vms"})
 List<StashProfileLinkEntity> findAllByStashProfile_User_Id(Long userId);
 Optional<StashProfileLinkEntity> findByFarmProfile_ProfileId(Long id);
 List<StashProfileLinkEntity> findAllByFarmProfile_ProfileIdIn(Collection<Long> ids);
 void deleteAllByStashProfile_ProfileId(Long id);

 @org.springframework.data.jpa.repository.Modifying
 @org.springframework.data.jpa.repository.Query("delete from StashProfileLinkEntity l where l.stashProfile.profileId = :id")
 int deleteLinksByStashId(@org.springframework.data.repository.query.Param("id") Long id);
}