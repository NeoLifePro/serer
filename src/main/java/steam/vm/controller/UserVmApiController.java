package steam.vm.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import steam.vm.dto.VmApiDtos;
import steam.vm.entity.ItemEntity;
import steam.vm.entity.ProfileEntity;
import steam.vm.entity.ProfileFarmTimeEntity;
import steam.vm.entity.UserEntity;
import steam.vm.repo.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UserVmApiController {
    private final UserRepository userRepo;
    private final ProfileRepository profileRepo;
    private final VmPanelRepository vmRepo;
    private final ItemRepository itemRepo;
    private final ProfileFarmTimeRepository farmTimeRepo;

    public UserVmApiController(UserRepository userRepo,
                               ProfileRepository profileRepo,
                               VmPanelRepository vmRepo,
                               ItemRepository itemRepo,
                               ProfileFarmTimeRepository farmTimeRepo) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.vmRepo = vmRepo;
        this.itemRepo = itemRepo;
        this.farmTimeRepo = farmTimeRepo;
    }


    @GetMapping("/item/add")
    public VmApiDtos.AddItemResponse addItemByGet(
            @RequestParam("profile_id") Long profileId,
            @RequestParam("item_name") String itemName,
            @RequestParam(value = "item_wear_and_tear", required = false) String itemWearAndTear,
            @RequestParam("item_price") Double itemPrice,
            @RequestParam(value = "created_at", required = false) java.time.LocalDateTime createdAt
    ) {
        VmApiDtos.AddItemRequest req = new VmApiDtos.AddItemRequest();
        req.profileId = profileId;
        req.itemName = itemName;
        req.itemWearAndTear = itemWearAndTear;
        req.itemPrice = itemPrice;
        req.createdAt = createdAt;
        return addItemInternal(req);
    }

    @GetMapping("/user/add")
    public VmApiDtos.AddItemResponse addItemByUserAddLink(
            @RequestParam("profile_id") Long profileId,
            @RequestParam("item_name") String itemName,
            @RequestParam(value = "item_wear_and_tear", required = false) String itemWearAndTear,
            @RequestParam("item_price") Double itemPrice,
            @RequestParam(value = "created_at", required = false) java.time.LocalDateTime createdAt
    ) {
        VmApiDtos.AddItemRequest req = new VmApiDtos.AddItemRequest();
        req.profileId = profileId;
        req.itemName = itemName;
        req.itemWearAndTear = itemWearAndTear;
        req.itemPrice = itemPrice;
        req.createdAt = createdAt;
        return addItemInternal(req);
    }

    private VmApiDtos.AddItemResponse addItemInternal(VmApiDtos.AddItemRequest req) {
        UserEntity user = currentUser();
        if (req.profileId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profile_id required");
        }

        if (req.itemName == null || req.itemName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item_name required");
        }
        if (req.itemPrice == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item_price required");
        }

        ProfileEntity profile = profileRepo.findByProfileIdAndUser_Id(req.profileId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profile not found"));

        ItemEntity item = new ItemEntity();
        item.setProfile(profile);
        item.setItemName(req.itemName.trim());
        item.setItemWearAndTear(trimToNull(req.itemWearAndTear));
        item.setItemPrice(req.itemPrice);
        item.setCreatedAt(req.createdAt);

        item = itemRepo.save(item);
        return new VmApiDtos.AddItemResponse(true, item.getItemId(), profile.getProfileId());
    }

    @GetMapping("/user/vm/profiles")
    public List<VmApiDtos.VmProfileResponse> vmProfiles(@RequestParam(required = false) String vmName) {
        UserEntity user = currentUser();
        String vmFilter = trimToNull(vmName);

        return vmRepo.findAllByProfile_User_Id(user.getId()).stream()
                .filter(vm -> vmFilter == null || vmFilter.equalsIgnoreCase(vm.getVmName()))
                .filter(vm -> vm.getProfile().getProfileType() == steam.vm.entity.ProfileType.FARM)
                .filter(vm -> !vm.getProfile().isFarmed())
                .filter(vm -> !vm.getProfile().isBlocked())
                .sorted(Comparator.comparing(vm -> safeLower(vm.getProfile().getLogin())))
                .map(vm -> new VmApiDtos.VmProfileResponse(
                        vm.getProfile().getProfileId(),
                        vm.getProfile().getLogin(),
                        vm.getVmName()
                ))
                .toList();
    }

    @GetMapping("/user/profile/password")
    public VmApiDtos.ProfilePasswordResponse profilePassword(@RequestParam String login) {
        UserEntity user = currentUser();
        ProfileEntity profile = profileRepo.findReadableByLoginAndUserId(login, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profile not found"));

        return new VmApiDtos.ProfilePasswordResponse(
                profile.getProfileId(),
                profile.getLogin(),
                profile.getParoleHash()
        );
    }

    @GetMapping("/user/profileid")
    public VmApiDtos.ProfileIdResponse profileId(@RequestParam String login) {
        UserEntity user = currentUser();
        ProfileEntity profile = profileRepo.findReadableByLoginAndUserId(login, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profile not found"));

        return new VmApiDtos.ProfileIdResponse(profile.getProfileId());
    }
    @GetMapping("/group/avgtime")
    public VmApiDtos.VmAvgFarmTime avgFarmTime(){
        UserEntity user = currentUser();

        Long time = farmTimeRepo.avgFarmTimeSecondsByUserId(user.getId());

        return new VmApiDtos.VmAvgFarmTime(time);
    }

    @PostMapping("/profile/farmtime")
    public VmApiDtos.FarmTimeResponse addFarmTime(@RequestBody VmApiDtos.FarmTimeRequest req) {
        UserEntity user = currentUser();

        if (req.profileId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profileId required");
        }

        if (req.farmTimeSeconds == null || req.farmTimeSeconds < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "farm_time_seconds required");
        }

        ProfileEntity profile = profileRepo.findByProfileIdAndUser_Id(req.profileId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profile not found"));

        ProfileFarmTimeEntity farmTime = new ProfileFarmTimeEntity();
        farmTime.setProfile(profile);
        farmTime.setFarmTimeSeconds(req.farmTimeSeconds);
        farmTime.setCreatedAt(req.createdAt);

        farmTime = farmTimeRepo.save(farmTime);

        profile.setFarmed(true);
        profileRepo.save(profile);

        return new VmApiDtos.FarmTimeResponse(
                true,
                farmTime.getFarmTimeId(),
                profile.getProfileId()
        );
    }

    @PostMapping("/profile/update")
    public VmApiDtos.ProfileUpdateResponse updateProfileFarmed(
            @RequestParam(required = false) String login,
            @RequestBody(required = false) VmApiDtos.ProfileUpdateRequest req
    ) {
        String profileLogin = trimToNull(login);
        if (profileLogin == null && req != null) {
            profileLogin = trimToNull(req.login);
        }
        return markProfileFarmed(profileLogin);
    }

    @GetMapping("/profile/update")
    public VmApiDtos.ProfileUpdateResponse updateProfileFarmedByGet(@RequestParam String login) {
        return markProfileFarmed(login);
    }

    private VmApiDtos.ProfileUpdateResponse markProfileFarmed(String login) {
        UserEntity user = currentUser();
        String profileLogin = trimToNull(login);
        if (profileLogin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "login required");
        }

        ProfileEntity profile = profileRepo.findReadableByLoginAndUserId(profileLogin, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profile not found"));

        profile.setFarmed(true);
        profile = profileRepo.save(profile);

        return new VmApiDtos.ProfileUpdateResponse(
                true,
                profile.getProfileId(),
                profile.getLogin(),
                profile.isFarmed()
        );
    }

    private ProfileEntity resolveProfile(UserEntity user, Long profileId, String profileLogin) {
        if (profileId != null) {
            return profileRepo.findByProfileIdAndUser_Id(profileId, user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profile not found"));
        }
        if (profileLogin != null && !profileLogin.isBlank()) {
            return profileRepo.findReadableByLoginAndUserId(profileLogin, user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profile not found"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profileId or profileLogin required");
    }

    private UserEntity currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }
        return userRepo.findReadableByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
