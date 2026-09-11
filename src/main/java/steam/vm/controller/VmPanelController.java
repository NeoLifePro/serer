package steam.vm.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import steam.vm.dto.VmPanelDtos;
import steam.vm.entity.ProfileEntity;
import steam.vm.entity.UserEntity;
import steam.vm.entity.VmPanelEntity;
import steam.vm.repo.ProfileRepository;
import steam.vm.repo.UserRepository;
import steam.vm.repo.VmPanelRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class VmPanelController {

    private final UserRepository userRepo;
    private final ProfileRepository profileRepo;
    private final VmPanelRepository vmRepo;

    public VmPanelController(UserRepository userRepo, ProfileRepository profileRepo, VmPanelRepository vmRepo) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.vmRepo = vmRepo;
    }

    //  список профилей текущего пользователя
    @GetMapping("/profiles/my")
    public List<VmPanelDtos.MyProfileRow> myProfiles() {
        UserEntity user = currentUser();

        //  берём профили напрямую из БД по user_id
        List<ProfileEntity> profiles = profileRepo.findAllByUser_Id(user.getId());

        return profiles.stream()
                .map(p -> new VmPanelDtos.MyProfileRow(
                        p.getProfileId(),
                        p.getLogin(),
                        p.getEmail(),
                        p.isFarmed(),
                        p.isBlocked(),
                        p.getSteamguard() != null
                ))
                .toList();
    }


    //  таблица: vm_name -> count, logins, last_time_started, active
    @GetMapping("/vm-panels/summary")
    public List<VmPanelDtos.VmPanelSummaryRow> summary() {
        UserEntity user = currentUser();
        List<VmPanelEntity> rows = vmRepo.findAllByProfile_User_Id(user.getId());

        // по vm_name
        Map<String, List<VmPanelEntity>> grouped = rows.stream()
                .filter(r -> r.getVmName() != null && !r.getVmName().isBlank())
                .collect(Collectors.groupingBy(VmPanelEntity::getVmName));

        LocalDateTime now = LocalDateTime.now();

        List<VmPanelDtos.VmPanelSummaryRow> out = new ArrayList<>();
        for (var e : grouped.entrySet()) {
            String vmName = e.getKey();
            List<VmPanelEntity> list = e.getValue();

            List<String> logins = list.stream()
                    .map(r -> r.getProfile() != null ? r.getProfile().getLogin() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();

            // берём максимальный last_time_started по этой панели
            LocalDateTime last = list.stream()
                    .map(VmPanelEntity::getLastTimeStarted)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            boolean active = false;
            if (last != null) {
                Duration d = Duration.between(last, now);
                active = !d.isNegative() && d.toMinutes() <= 30; // активна, если <=30 минут назад
            }

            out.add(new VmPanelDtos.VmPanelSummaryRow(vmName, active, logins.size(), logins, last));
        }

        // сортировка: active сверху, потом по имени
        out.sort(Comparator
                .comparing((VmPanelDtos.VmPanelSummaryRow r) -> !r.active)
                .thenComparing(r -> r.vmName));

        return out;
    }

    // привязать профиль к панели
    @PostMapping("/vm-panels/assign")
    public String assign(@RequestBody VmPanelDtos.VmPanelAssignRequest req) {
        UserEntity user = currentUser();

        if (req == null || req.profileLogin == null || req.profileLogin.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profileLogin required");
        if (req.vmName == null || req.vmName.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vmName required");

        ProfileEntity profile = profileRepo.findReadableByLoginAndUserId(req.profileLogin, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profile not found for this user"));

        // upsert: в таблице vm_panel можно держать 1 запись на профиль
        VmPanelEntity vm = vmRepo.findByProfile_ProfileId(profile.getProfileId())
                .orElseGet(VmPanelEntity::new);

        vm.setProfile(profile);
        vm.setVmName(req.vmName.trim());
        if (vm.getLastTimeStarted() == null) {
            vm.setLastTimeStarted(LocalDateTime.now());
        }

        vmRepo.save(vm);
        return "assigned";
    }

    //  отвязать профиль от панели
    @DeleteMapping("/vm-panels/unassign")
    public String unassign(@RequestParam String profileLogin) {
        UserEntity user = currentUser();
        if (profileLogin == null || profileLogin.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profileLogin required");

        ProfileEntity profile = profileRepo.findReadableByLoginAndUserId(profileLogin, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profile not found for this user"));

        VmPanelEntity vm = vmRepo.findByProfile_ProfileId(profile.getProfileId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vm_panel row not found"));

        vmRepo.delete(vm);
        return "unassigned";
    }

    @DeleteMapping("/vm-panels/by-name")
    public Map<String, Object> deleteByName(@RequestParam String vmName) {
        UserEntity user = currentUser();
        if (vmName == null || vmName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vmName required");
        }

        int deleted = vmRepo.deleteAllByUserIdAndVmName(user.getId(), vmName.trim());
        return Map.of(
                "ok", true,
                "deleted", deleted
        );
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
