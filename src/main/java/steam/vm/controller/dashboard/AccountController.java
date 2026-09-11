package steam.vm.controller.dashboard;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import steam.vm.dto.AccountDtos;
import steam.vm.dto.AccountEditDtos;
import steam.vm.dto.AccountRow;
import steam.vm.entity.ProfileEntity;
import steam.vm.entity.SteamGuardEntity;
import steam.vm.entity.UserEntity;
import steam.vm.repo.ProfileRepository;
import steam.vm.repo.SteamGuardRepository;
import steam.vm.repo.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final UserRepository userRepo;
    private final ProfileRepository profileRepo;
    private final SteamGuardRepository steamGuardRepo;

    public AccountController(UserRepository userRepo,
                             ProfileRepository profileRepo,
                             SteamGuardRepository steamGuardRepo) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.steamGuardRepo = steamGuardRepo;
    }

    @PostMapping("/accounts/farm-zero-all")
    public Map<String, Object> resetAllFarmed() {
        UserEntity user = currentUser();
        int updated = profileRepo.resetAllFarmed(user.getId());

        return Map.of(
                "ok", true,
                "updated", updated
        );
    }

    @GetMapping("/accounts/{profileId}")
    public AccountEditDtos.AccountDetailsResponse getAccount(@PathVariable Long profileId) {
        UserEntity user = currentUser();

        ProfileEntity profile = profileRepo.findByProfileIdAndUser_Id(profileId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profile not found"));

        AccountEditDtos.AccountDetailsResponse dto = new AccountEditDtos.AccountDetailsResponse();
        dto.profileId = profile.getProfileId();
        dto.login = profile.getLogin();
        dto.email = profile.getEmail();
        dto.paroleHash = profile.getParoleHash();
        dto.farmed = profile.isFarmed();
        dto.blocked = profile.isBlocked();

        steamGuardRepo.findByProfile_ProfileId(profileId).ifPresentOrElse(guard -> {
            dto.hasSteamGuard = true;
            dto.steamid64 = guard.getSteamid64();
            dto.sharedSecret = guard.getSharedSecret();
            dto.identitySecret = guard.getIdentitySecret();
            dto.deviceId = guard.getDeviceId();
        }, () -> {
            dto.hasSteamGuard = false;
        });

        return dto;
    }

    @PutMapping("/accounts/{profileId}")
    public Map<String, Object> updateAccount(@PathVariable Long profileId,
                                             @RequestBody AccountEditDtos.UpdateAccountRequest req) {
        UserEntity user = currentUser();

        ProfileEntity profile = profileRepo.findByProfileIdAndUser_Id(profileId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profile not found"));

        if (req.login != null) {
            profile.setLogin(req.login.trim());
        }

        if (req.email != null) {
            profile.setEmail(req.email.trim());
        }

        if (req.paroleHash != null) {
            profile.setParoleHash(req.paroleHash);
        }

        if (req.farmed != null) {
            profile.setFarmed(req.farmed);
        }

        if (req.blocked != null) {
            profile.setBlocked(req.blocked);
        }

        profileRepo.save(profile);

        if (Boolean.TRUE.equals(req.hasSteamGuard)) {
            SteamGuardEntity guard = steamGuardRepo.findByProfile_ProfileId(profileId)
                    .orElseGet(() -> {
                        SteamGuardEntity g = new SteamGuardEntity();
                        g.setProfile(profile);
                        return g;
                    });

            if (req.steamid64 != null) guard.setSteamid64(req.steamid64.trim());
            if (req.sharedSecret != null) guard.setSharedSecret(req.sharedSecret.trim());
            if (req.identitySecret != null) guard.setIdentitySecret(req.identitySecret.trim());
            if (req.deviceId != null) guard.setDeviceId(req.deviceId.trim());

            steamGuardRepo.save(guard);
        }

        return Map.of(
                "ok", true,
                "profileId", profileId
        );
    }

    @GetMapping("/accounts")
    public AccountDtos.PageResponse<AccountRow> accounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String login,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String vmName
    ) {
        UserEntity user = currentUser();

        final String loginFilter = norm(login);
        final String emailFilter = norm(email);
        final String vmNameFilter = norm(vmName);

        if (size < 1) size = 25;
        if (size > 100) size = 100;
        if (page < 0) page = 0;

        List<AccountRow> filtered = profileRepo.findAccountRowsByUserId(user.getId()).stream()
                .filter(row -> containsIgnoreCase(row.login, loginFilter))
                .filter(row -> containsIgnoreCase(row.email, emailFilter))
                .filter(row -> containsIgnoreCase(row.vmName, vmNameFilter))
                .sorted(Comparator.comparing(row -> safeLower(row.login)))
                .toList();

        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<AccountRow> content = filtered.subList(from, to);
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / size);

        return new AccountDtos.PageResponse<>(
                content,
                page,
                size,
                filtered.size(),
                totalPages
        );
    }

    @GetMapping("/accounts/info")
    public AccountDtos.AccountCountResponse accountCount()
    {
        UserEntity user = currentUser();

        long total = profileRepo.countByUser_IdAndProfileType(user.getId(), steam.vm.entity.ProfileType.FARM);

        long farmed = profileRepo.countByUser_IdAndProfileTypeAndFarmedTrue(user.getId(), steam.vm.entity.ProfileType.FARM);

        long needFarm = profileRepo.countByUser_IdAndProfileTypeAndFarmedFalse(user.getId(), steam.vm.entity.ProfileType.FARM);

        long blocked = profileRepo.countByUser_IdAndProfileTypeAndBlockedTrue(user.getId(), steam.vm.entity.ProfileType.FARM);

        return new AccountDtos.AccountCountResponse(total, farmed, needFarm, blocked);
    }

    private String norm(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private boolean containsIgnoreCase(String value, String query) {
        return query == null || safeLower(value).contains(query.toLowerCase());
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
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
