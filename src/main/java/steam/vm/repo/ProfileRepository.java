package steam.vm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import steam.vm.dto.AccountRow;
import steam.vm.entity.ProfileEntity;
import steam.vm.entity.ProfileType;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {

    // ===============================
    // BASIC SEARCH
    // ===============================

    // Находит профиль по login и user_id
    Optional<ProfileEntity> findByLoginAndUser_Id(String login, Long userId);

    // Возвращает все профили конкретного пользователя
    List<ProfileEntity> findAllByUser_Id(Long userId);
    List<ProfileEntity> findAllByUser_IdAndProfileType(Long userId, ProfileType profileType);

    @Query("""
        select distinct p from ProfileEntity p
        left join fetch p.vms
        where p.user.id = :userId and p.profileType = :profileType
    """)
    List<ProfileEntity> findStorageProfiles(
            @Param("userId") Long userId,
            @Param("profileType") ProfileType profileType
    );

    List<ProfileEntity> findAllByProfileIdInAndUser_Id(List<Long> profileIds, Long userId);

    default Optional<ProfileEntity> findReadableByLoginAndUserId(String login, Long userId) {
        return findByLoginAndUser_Id(login, userId)
                .or(() -> findAllByUser_Id(userId).stream()
                        .filter(profile -> login != null && login.equals(profile.getLogin()))
                        .findFirst());
    }

    Optional<ProfileEntity> findByProfileIdAndUser_Id(Long profileId, Long userId);


    // ===============================
    // ACCOUNTS PAGE
    // ===============================
    @Query("""
        select new steam.vm.dto.AccountRow(
            p.profileId,
            p.login,
            p.email,
            p.paroleHash,
            p.farmed,
            p.blocked,
            case when sg.guardId is not null then true else false end,
            coalesce((select sum(i.itemPrice) from ItemEntity i where i.profile = p), 0),
            v.vmName,
            v.lastTimeStarted
        )
        from ProfileEntity p
        left join VmPanelEntity v on v.profile = p
        left join SteamGuardEntity sg on sg.profile = p
        where p.user.id = :userId and p.profileType = steam.vm.entity.ProfileType.FARM
    """)
    List<AccountRow> findAccountRowsByUserId(@Param("userId") Long userId);


    // ===============================
    // FARM RESET
    // ===============================

    /*
        Сбрасывает farmed = false у всех аккаунтов пользователя.
    */
    @Modifying
    @Transactional
    @Query("""
        update ProfileEntity p
        set p.farmed = false
        where p.user.id = :userId and p.profileType = steam.vm.entity.ProfileType.FARM
    """)
    int resetAllFarmed(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = """
        delete from profile
        where profile_id = :profileId
          and user_id = :userId
          and profile_type = 'STASH'
    """, nativeQuery = true)
    int deleteStashDirect(
            @Param("profileId") Long profileId,
            @Param("userId") Long userId
    );

    @Modifying
    @Transactional
    @Query("""
        update ProfileEntity p
        set p.farmed = false
    """)
    int resetAllFarmed();


    // ===============================
    // DASHBOARD COUNTS
    // ===============================

    long countByUser_Id(Long userId);

    long countByUser_IdAndFarmedTrue(Long userId);

    long countByUser_IdAndFarmedFalse(Long userId);

    long countByUser_IdAndBlockedTrue(Long userId);
    long countByUser_IdAndProfileType(Long userId, ProfileType profileType);
    long countByUser_IdAndProfileTypeAndFarmedTrue(Long userId, ProfileType profileType);
    long countByUser_IdAndProfileTypeAndFarmedFalse(Long userId, ProfileType profileType);
    long countByUser_IdAndProfileTypeAndBlockedTrue(Long userId, ProfileType profileType);

}
