package steam.vm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import steam.vm.entity.ProfileFarmTimeEntity;

import java.time.LocalDateTime;

public interface ProfileFarmTimeRepository extends JpaRepository<ProfileFarmTimeEntity, Long> {

    @Query("""
        select avg(f.farmTimeSeconds)
        from ProfileFarmTimeEntity f
        where f.profile.user.id = :userId
    """)
    Long avgFarmTimeSecondsByUserId(@Param("userId") Long userId);

    @Query("""
        select avg(f.farmTimeSeconds)
        from ProfileFarmTimeEntity f
        where f.profile.user.id = :userId
          and f.createdAt >= :from
    """)
    Double avgFarmTimeSecondsByUserIdFrom(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from
    );

    @Query("""
        select count(f)
        from ProfileFarmTimeEntity f
        where f.profile.user.id = :userId
    """)
    Long countFarmedProfilesByUserId(@Param("userId") Long userId);
}