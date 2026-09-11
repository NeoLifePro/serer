package steam.vm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import steam.vm.entity.ItemEntity;
import steam.vm.dto.lastdrop.WeeklyDropItemRow;

import java.time.LocalDateTime;
import java.util.List;


public interface ItemRepository extends JpaRepository<ItemEntity, Long> {

    /*
        Считает сумму itemPrice
    */
    @Query("""
    select coalesce(sum(i.itemPrice), 0)
    from ItemEntity i
    where i.profile.user.id = :userId
      and i.profile.profileType = steam.vm.entity.ProfileType.FARM
      and i.createdAt >= :fromDate
""")
    Double sumItemPriceByUserIdFromDate(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate
    );

    @Query("""
        select count(i.itemId)
        from ItemEntity i
        where i.profile.user.id = :userId
          and i.profile.profileType = steam.vm.entity.ProfileType.FARM
          and i.createdAt >= :fromDate
   """)
    long CountItemByUserId(@Param("userId") long userId, @Param("fromDate") LocalDateTime fromDate);

    @Query(value = """
    SELECT COUNT(*)
    FROM (
        SELECT DISTINCT
            i.item_name,
            COALESCE(i.item_wear_and_tear, '')
        FROM items i
        INNER JOIN profile p
            ON i.profile_id = p.profile_id
        WHERE p.user_id = :userId
          AND p.profile_type = 'FARM'
    ) AS unique_items
""", nativeQuery = true)
    long countUniqueItemsByUserId(@Param("userId") long userId);


    interface UniqueItemProjection {
        String getItemName();
        String getItemWearAndTear();
        Long getItemCount();
        Double getTotalPrice();
        Double getAvgPrice();
    }

    @Query(value = """
        SELECT
            i.item_name AS itemName,
            i.item_wear_and_tear AS itemWearAndTear,
            COUNT(*) AS itemCount,
            ROUND(SUM(i.item_price), 2) AS totalPrice,
            ROUND(AVG(i.item_price), 2) AS avgPrice
        FROM items i
        INNER JOIN profile p
            ON i.profile_id = p.profile_id
        WHERE p.user_id = :userId
          AND p.profile_type = 'FARM'
          AND i.created_at >= :fromDate
        GROUP BY
            i.item_name,
            i.item_wear_and_tear
        ORDER BY itemCount DESC
    """, nativeQuery = true)
    List<UniqueItemProjection> findUniqueItemsByUserIdFromDate(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate
    );

    @Query("""
        select new steam.vm.dto.lastdrop.WeeklyDropItemRow(
            i.itemName,
            i.itemWearAndTear,
            i.profile.login,
            count(i.itemId),
            coalesce(sum(i.itemPrice), 0.0),
            coalesce(avg(i.itemPrice), 0.0)
        )
        from ItemEntity i
        where i.profile.user.id = :userId
          and i.profile.profileType = steam.vm.entity.ProfileType.FARM
          and i.createdAt >= :fromDate
        group by i.itemName, i.itemWearAndTear, i.profile.login
        order by count(i.itemId) desc, i.itemName asc
    """)
    List<WeeklyDropItemRow> findWeeklyDropItemsByUserIdFromDate(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate
    );

}
