package steam.vm.controller.lastdrop;


import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import steam.vm.dto.lastdrop.LastdropDto;
import steam.vm.dto.lastdrop.WeeklyDropItemRow;
import steam.vm.entity.UserEntity;
import steam.vm.repo.ItemRepository;
import steam.vm.repo.ProfileRepository;
import steam.vm.repo.UserRepository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/lastdrop")
public class LastDropController {
     private final ItemRepository itemRepo;
     private final UserRepository userRepo;
     private final ProfileRepository profileRepo;

     public LastDropController(ItemRepository itemRepo, UserRepository userRepo, ProfileRepository profileRepo){
         this.itemRepo = itemRepo;
         this.userRepo = userRepo;
         this.profileRepo = profileRepo;
     }

    @GetMapping("/infopanel")
    public LastdropDto.LastDropInfoPanelResponse lastDropInfoPanelResponse(){
        UserEntity user = currentUser();
        LocalDateTime weekStart = getCurrentDropWeekStart();
        Double itemsSum = itemRepo.sumItemPriceByUserIdFromDate(user.getId(), weekStart);
        long itemCount = itemRepo.CountItemByUserId(user.getId(), weekStart );
        long uniqItems = itemRepo.countUniqueItemsByUserId(user.getId());

        return new LastdropDto.LastDropInfoPanelResponse(itemCount, itemsSum, uniqItems, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }


    @GetMapping("/unique-items")
    public List<ItemRepository.UniqueItemProjection> uniqueItems() {
        UserEntity user = currentUser();
        LocalDateTime weekStart = getCurrentDropWeekStart();

        return itemRepo.findUniqueItemsByUserIdFromDate(user.getId(), weekStart);
    }

    @GetMapping("/weekly-items")
    public List<WeeklyDropItemRow> weeklyItems() {
        UserEntity user = currentUser();
        LocalDateTime weekStart = getCurrentDropWeekStart();

        return itemRepo.findWeeklyDropItemsByUserIdFromDate(user.getId(), weekStart);
    }

    private UserEntity currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }
        return userRepo.findReadableByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }


    private LocalDateTime getCurrentDropWeekStart() {
        LocalDateTime now = LocalDateTime.now();

    /*
        Java DayOfWeek:
        MONDAY = 1
        TUESDAY = 2
        WEDNESDAY = 3
        THURSDAY = 4
        FRIDAY = 5
        SATURDAY = 6
        SUNDAY = 7
    */

        LocalDateTime resetTime = now
                .withHour(4)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        int currentDay = now.getDayOfWeek().getValue();
        int resetDay = DayOfWeek.WEDNESDAY.getValue();

        int daysSinceWednesday = (currentDay - resetDay + 7) % 7;

        resetTime = resetTime.minusDays(daysSinceWednesday);

    /*
        Если сегодня среда, но сейчас ещё раньше 04:00,
        значит текущая farm-неделя началась в прошлую среду.
    */
        if (now.getDayOfWeek() == DayOfWeek.WEDNESDAY && now.isBefore(resetTime)) {
            resetTime = resetTime.minusDays(7);
        }

        return resetTime;
    }


}
