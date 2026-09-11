package steam.vm.controller.dashboard;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import steam.vm.dto.StashDtos;
import steam.vm.entity.*;
import steam.vm.repo.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stashes")
public class StashController {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final StashProfileLinkRepository links;
    private final SteamGuardRepository guards;
    private final JdbcTemplate jdbc;

    public StashController(UserRepository users, ProfileRepository profiles,
                           StashProfileLinkRepository links, SteamGuardRepository guards,
                           JdbcTemplate jdbc) {
        this.users = users;
        this.profiles = profiles;
        this.links = links;
        this.guards = guards;
        this.jdbc = jdbc;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<StashDtos.StashAccount> list() {
        return loadSnapshot(user()).stashes();
    }

    @GetMapping("/snapshot")
    @Transactional(readOnly = true)
    public StashDtos.StorageSnapshot snapshot() {
        return loadSnapshot(user());
    }

    private StashDtos.StorageSnapshot loadSnapshot(UserEntity user) {
        Long userId = user.getId();
        List<ProfileEntity> stashes = profiles.findStorageProfiles(userId, ProfileType.STASH);
        List<ProfileEntity> farms = profiles.findStorageProfiles(userId, ProfileType.FARM);
        List<StashProfileLinkEntity> allLinks = links.findAllByStashProfile_User_Id(userId);

        Map<Long, List<StashProfileLinkEntity>> byStash = allLinks.stream()
                .collect(Collectors.groupingBy(link -> link.getStashProfile().getProfileId()));
        Set<Long> guarded = new HashSet<>(guards.findGuardedProfileIds(userId));

        List<StashDtos.StashAccount> stashRows = stashes.stream()
                .sorted(Comparator.comparing(ProfileEntity::getLogin, String.CASE_INSENSITIVE_ORDER))
                .map(stash -> dto(stash,
                        byStash.getOrDefault(stash.getProfileId(), List.of()),
                        guarded.contains(stash.getProfileId())))
                .toList();

        List<StashDtos.FarmAccount> farmRows = farms.stream()
                .sorted(Comparator.comparing(ProfileEntity::getLogin, String.CASE_INSENSITIVE_ORDER))
                .map(this::farmDto)
                .toList();

        return new StashDtos.StorageSnapshot(stashRows, farmRows);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public StashDtos.StashAccount create(@RequestBody StashDtos.SaveRequest request) {
        UserEntity user = user();
        if (blank(request.login) || blank(request.email) || blank(request.password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "login, email and password are required");
        }
        ProfileEntity stash = new ProfileEntity();
        stash.setUser(user);
        stash.setProfileType(ProfileType.STASH);
        stash.setLogin(request.login.trim());
        stash.setEmail(request.email.trim());
        stash.setParoleHash(request.password);
        stash.setBlocked(Boolean.TRUE.equals(request.blocked));
        stash = profiles.save(stash);
        replace(user, stash, request.farmProfileIds);
        return dto(stash);
    }

    @PutMapping("/{id}")
    @Transactional
    public StashDtos.StashAccount update(@PathVariable Long id, @RequestBody StashDtos.SaveRequest request) {
        UserEntity user = user();
        ProfileEntity stash = stash(user, id);
        if (!blank(request.email)) stash.setEmail(request.email.trim());
        if (!blank(request.password)) stash.setParoleHash(request.password);
        if (request.blocked != null) stash.setBlocked(request.blocked);
        profiles.save(stash);
        replace(user, stash, request.farmProfileIds);
        return dto(stash);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, Object> delete(@PathVariable Long id) {
        UserEntity user = user();
        long released = links.countByStashProfile_ProfileId(id);
        links.deleteLinksByStashId(id);
        int deleted = profiles.deleteStashDirect(id, user.getId());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "stash not found");
        }
        return Map.of("ok", true, "released", released);
    }

    @GetMapping("/assignment")
    @Transactional(readOnly = true)
    public StashDtos.Assignment assignment(@RequestParam Long farmProfileId) {
        UserEntity user = user();
        ProfileEntity farm = profiles.findByProfileIdAndUser_Id(farmProfileId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "farm profile not found"));
        if (farm.getProfileType() != ProfileType.FARM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profile is not FARM");
        }
        return links.findByFarmProfile_ProfileId(farmProfileId)
                .map(link -> new StashDtos.Assignment(
                        link.getStashProfile().getProfileId(),
                        link.getStashProfile().getLogin(),
                        link.getStashProfile().getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "stash is not assigned"));
    }

    @GetMapping("/trade-config")
    @Transactional(readOnly = true)
    public StashDtos.TradeConfig tradeConfig(@RequestParam(required = false) String vmName) {
        UserEntity user = user();
        Map<Long, List<StashProfileLinkEntity>> groups = links.findAllByStashProfile_User_Id(user.getId()).stream()
                .filter(link -> vmName == null || vmName.isBlank() || link.getFarmProfile().getVms().stream()
                        .anyMatch(vm -> vmName.equalsIgnoreCase(vm.getVmName())))
                .collect(Collectors.groupingBy(link -> link.getStashProfile().getProfileId()));

        List<StashDtos.TradeGroup> result = new ArrayList<>();
        for (List<StashProfileLinkEntity> group : groups.values()) {
            ProfileEntity stash = group.get(0).getStashProfile();
            List<StashDtos.TradeProfile> farms = group.stream()
                    .map(link -> tradeProfile(link.getFarmProfile()))
                    .toList();
            result.add(new StashDtos.TradeGroup(tradeProfile(stash), farms));
        }
        return new StashDtos.TradeConfig(result);
    }

    private StashDtos.TradeProfile tradeProfile(ProfileEntity profile) {
        SteamGuardEntity guard = guards.findByProfile_ProfileId(profile.getProfileId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "Steam Guard missing for " + profile.getLogin()));
        return new StashDtos.TradeProfile(profile.getProfileId(), profile.getLogin(), profile.getParoleHash(),
                guard.getSteamid64(), guard.getSharedSecret(), guard.getIdentitySecret(), guard.getTradeToken());
    }

    private void replace(UserEntity user, ProfileEntity stash, List<Long> rawIds) {
        List<Long> ids = rawIds == null ? List.of() : rawIds.stream()
                .filter(Objects::nonNull).distinct().toList();

        List<ProfileEntity> farms = ids.isEmpty()
                ? List.of()
                : profiles.findAllByProfileIdInAndUser_Id(ids, user.getId());
        Map<Long, ProfileEntity> farmsById = farms.stream()
                .collect(Collectors.toMap(ProfileEntity::getProfileId, Function.identity()));

        for (Long id : ids) {
            ProfileEntity farm = farmsById.get(id);
            if (farm == null || farm.getProfileType() != ProfileType.FARM) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "farm profile not found: " + id);
            }
        }

        if (!ids.isEmpty()) {
            for (StashProfileLinkEntity existing : links.findAllByFarmProfile_ProfileIdIn(ids)) {
                if (!existing.getStashProfile().getProfileId().equals(stash.getProfileId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "farm profile already assigned: " + existing.getFarmProfile().getProfileId());
                }
            }
        }

        links.deleteLinksByStashId(stash.getProfileId());

        if (!ids.isEmpty()) {
            String values = String.join(",", Collections.nCopies(ids.size(), "(?, ?)"));
            Object[] parameters = new Object[ids.size() * 2];
            for (int index = 0; index < ids.size(); index++) {
                parameters[index * 2] = stash.getProfileId();
                parameters[index * 2 + 1] = ids.get(index);
            }
            jdbc.update(
                    "insert into stash_profile_link (stash_profile_id, farm_profile_id) values " + values,
                    parameters
            );
        }
    }

    private StashDtos.FarmAccount farmDto(ProfileEntity farm) {
        String vmName = farm.getVms().stream().findFirst().map(VmPanelEntity::getVmName).orElse(null);
        return new StashDtos.FarmAccount(
                farm.getProfileId(), farm.getLogin(), vmName, farm.isFarmed(), farm.isBlocked());
    }

    private StashDtos.StashAccount dto(ProfileEntity stash,
                                        List<StashProfileLinkEntity> stashLinks,
                                        boolean guarded) {
        List<StashDtos.FarmAccount> accounts = stashLinks.stream()
                .map(link -> farmDto(link.getFarmProfile()))
                .toList();
        return new StashDtos.StashAccount(stash.getProfileId(), stash.getLogin(), stash.getEmail(),
                stash.isBlocked(), guarded, accounts);
    }

    private StashDtos.StashAccount dto(ProfileEntity stash) {
        List<StashProfileLinkEntity> stashLinks = links.findAllByStashProfile_ProfileId(stash.getProfileId());
        return dto(stash, stashLinks, guards.findByProfile_ProfileId(stash.getProfileId()).isPresent());
    }

    private ProfileEntity stash(UserEntity user, Long id) {
        ProfileEntity profile = profiles.findByProfileIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "stash not found"));
        if (profile.getProfileType() != ProfileType.STASH) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "stash not found");
        }
        return profile;
    }

    private UserEntity user() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return users.findReadableByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}