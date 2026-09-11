package steam.vm.controller.dashboard;



import java.time.DayOfWeek;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import steam.vm.dto.ItemDtos;

import steam.vm.entity.ProfileEntity;
import steam.vm.entity.UserEntity;
import steam.vm.repo.ItemRepository;
import steam.vm.repo.ProfileRepository;
import steam.vm.repo.ItemRepository;
import steam.vm.repo.UserRepository;


@RestController
@RequestMapping("/api/item/")
public class ItemAndPriceController {

    private final ItemRepository itemRepo;
    private final UserRepository userRepo;
    private final ProfileRepository profileRepo;

    ItemAndPriceController(ItemRepository itemRepo, ProfileRepository profileRepo , UserRepository userRepo){
        this.itemRepo = itemRepo;
        this.profileRepo = profileRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/drop/prices")
    public ItemDtos.ItemPriceResponse itemPricesResponse(){
        UserEntity user = currentUser();

        long farmedProfiles = profileRepo.countByUser_IdAndFarmedTrue(user.getId());

        if(farmedProfiles == 0){
            return new ItemDtos.ItemPriceResponse(0.0);
        }

        LocalDateTime weekStart = getCurrentDropWeekStart();

        Double totalPrice = itemRepo.sumItemPriceByUserIdFromDate(user.getId(), weekStart);

        if(totalPrice == null){
            totalPrice = 0.0;
        }
        totalPrice = Double.valueOf(String.format("%.2f", totalPrice));
        return new ItemDtos.ItemPriceResponse(totalPrice);
    }

     //alter table items
    //add column created_at datetime not null default current_timestamp;
    @GetMapping("/drop/avg")
    public ItemDtos.ItemAvgPriceResponse itemAvgPriceResponse(){
        UserEntity user = currentUser();

        long farmedProfiles = profileRepo.countByUser_IdAndFarmedTrue(user.getId());

        if (farmedProfiles == 0){
            return new ItemDtos.ItemAvgPriceResponse(0.0);
        }

        LocalDateTime weekStart = getCurrentDropWeekStart();

        Double totalPrice = itemRepo.sumItemPriceByUserIdFromDate(user.getId(), weekStart);


        double dropAvg = Double.parseDouble(String.format("%.2f", totalPrice / farmedProfiles));

        return new ItemDtos.ItemAvgPriceResponse((dropAvg));

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

        if (now.getDayOfWeek() == DayOfWeek.WEDNESDAY && now.isBefore(resetTime)) {
            resetTime = resetTime.minusDays(7);
        }

        return resetTime;
    }
    private UserEntity currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }
        return userRepo.findReadableByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }
}
