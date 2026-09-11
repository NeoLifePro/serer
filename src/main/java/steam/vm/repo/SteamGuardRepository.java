package steam.vm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import steam.vm.entity.SteamGuardEntity;

import java.util.List;
import java.util.Optional;

public interface SteamGuardRepository extends JpaRepository<SteamGuardEntity, Long> {
    Optional<SteamGuardEntity> findByProfile_ProfileId(Long profileId);
    Optional<SteamGuardEntity> findBySteamid64(String steamid64);
    List<SteamGuardEntity> findAllByProfile_User_IdOrderByGuardIdAsc(Long userId);

    @org.springframework.data.jpa.repository.Query("""
        select g.profile.profileId from SteamGuardEntity g
        where g.profile.user.id = :userId
    """)
    java.util.List<Long> findGuardedProfileIds(
            @org.springframework.data.repository.query.Param("userId") Long userId
    );

    default Optional<SteamGuardEntity> findReadableBySteamid64(String steamid64) {
        return findBySteamid64(steamid64)
                .or(() -> findAll().stream()
                        .filter(guard -> steamid64 != null && steamid64.equals(guard.getSteamid64()))
                        .findFirst());
    }
}
