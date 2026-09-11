package steam.vm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import steam.vm.entity.ProfileEntity;
import steam.vm.entity.SteamGuardEntity;
import steam.vm.entity.UserEntity;
import steam.vm.repo.ProfileRepository;
import steam.vm.repo.SteamGuardRepository;
import steam.vm.repo.UserRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/steamguard")
public class SteamGuardController {

    private final ProfileRepository profileRepo;
    private final SteamGuardRepository sgRepo;
    private final UserRepository userRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final char[] STEAM_CODE_CHARS = "23456789BCDFGHJKMNPQRTVWXY".toCharArray();

    public SteamGuardController(ProfileRepository profileRepo,
                                SteamGuardRepository sgRepo,
                                UserRepository userRepo) {
        this.profileRepo = profileRepo;
        this.sgRepo = sgRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/all/steamid64")
    public Map<String, Object> allSteamId64() {
        UserEntity user = currentUser();

        List<String> steamIds = sgRepo.findAllByProfile_User_IdOrderByGuardIdAsc(user.getId()).stream()
                .map(SteamGuardEntity::getSteamid64)
                .toList();

        return Map.of("steamId", steamIds);
    }

    @GetMapping("/code")
    public java.util.Map<String, Object> code(
            @RequestParam(required = false) Long profileId,
            @RequestParam(required = false) String profileLogin,
            @RequestParam(required = false) String login
    ) {
        UserEntity user = currentUser();
        String requestedLogin = profileLogin != null && !profileLogin.isBlank() ? profileLogin : login;

        ProfileEntity profile;
        if (profileId != null) {
            profile = profileRepo.findByProfileIdAndUser_Id(profileId, user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        } else if (requestedLogin != null && !requestedLogin.isBlank()) {
            profile = profileRepo.findReadableByLoginAndUserId(requestedLogin, user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profileId or profileLogin required");
        }

        SteamGuardEntity guard = sgRepo.findByProfile_ProfileId(profile.getProfileId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SteamGuard not found"));

        String code = generateSteamCode(guard.getSharedSecret(), System.currentTimeMillis() / 1000L);
        return java.util.Map.of(
                "profileId", profile.getProfileId(),
                "login", profile.getLogin(),
                "code", code
        );
    }

    @PostMapping("/import-by-login")
    public java.util.Map<String, Object> importByLogin(
            @RequestParam("profileLogin") String profileLogin,
            @RequestParam("mafile") MultipartFile mafile,
            @RequestParam(value = "tradeToken", required = false) String tradeToken
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not logged in");
        }

        UserEntity user = userRepo.findReadableByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (profileLogin == null || profileLogin.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profileLogin required");
        }

        if (mafile == null || mafile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maFile is empty");
        }

        ProfileEntity profile = profileRepo
                .findReadableByLoginAndUserId(profileLogin, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Profile not found for user: " + profileLogin
                ));

        try {
            String jsonText = new String(mafile.getBytes(), StandardCharsets.UTF_8);
            JsonNode root = mapper.readTree(jsonText);

            String steamID64 = null;
            if (root.hasNonNull("SteamID")) {
                steamID64 = root.get("SteamID").asText();
            } else if (root.has("Session") && root.get("Session").hasNonNull("SteamID")) {
                steamID64 = root.get("Session").get("SteamID").asText();
            }

            String sharedSecret = pick(root, "shared_secret");
            String identitySecret = pick(root, "identity_secret");
            String deviceId = pickNullable(root, "device_id");

            if (sharedSecret == null || identitySecret == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "maFile missing shared_secret or identity_secret"
                );
            }

            if (steamID64 == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot detect steamid64"
                );
            }

            var existingSteam = sgRepo.findReadableBySteamid64(steamID64);

            if (existingSteam.isPresent()) {
                SteamGuardEntity existing = existingSteam.get();
                Long existingProfileId = existing.getProfile() != null
                        ? existing.getProfile().getProfileId()
                        : null;

                if (existingProfileId != null && !existingProfileId.equals(profile.getProfileId())) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "SteamGuard already attached to profileId=" + existingProfileId
                    );
                }
            }

            SteamGuardEntity sg = sgRepo
                    .findByProfile_ProfileId(profile.getProfileId())
                    .orElseGet(SteamGuardEntity::new);

            sg.setProfile(profile);
            profile.setSteamguard(sg);
            sg.setSteamid64(steamID64);
            sg.setSharedSecret(sharedSecret);
            sg.setIdentitySecret(identitySecret);
            sg.setDeviceId(deviceId);
            if (tradeToken != null && !tradeToken.isBlank()) sg.setTradeToken(normalizeTradeToken(tradeToken));

            SteamGuardEntity saved = sgRepo.saveAndFlush(sg);

            return java.util.Map.of(
                    "success", true,
                    "message", "SteamGuard saved",
                    "guardId", saved.getGuardId(),
                    "profileId", profile.getProfileId(),
                    "steamid64", saved.getSteamid64()
            );

        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Database constraint error: " + e.getMostSpecificCause().getMessage()
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot parse maFile: " + e.getMessage()
            );
        }
    }

    private String pick(JsonNode root, String... keys) {

        for (String k : keys) {

            if (root.hasNonNull(k) && !root.get(k).asText().isBlank()) {
                return root.get(k).asText();
            }

        }

        return null;
    }

    private String pickNullable(JsonNode root, String key) {

        if (root.hasNonNull(key)) {

            String value = root.get(key).asText();

            if (!value.isBlank()) {
                return value;
            }

        }

        return null;
    }

    private UserEntity currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not logged in");
        }

        return userRepo.findReadableByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String generateSteamCode(String sharedSecret, long unixTimeSeconds) {
        try {
            byte[] secret = Base64.getDecoder().decode(sharedSecret);
            byte[] time = ByteBuffer.allocate(8).putLong(unixTimeSeconds / 30L).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hmac = mac.doFinal(time);

            int offset = hmac[hmac.length - 1] & 0x0F;
            int fullCode = ((hmac[offset] & 0x7F) << 24)
                    | ((hmac[offset + 1] & 0xFF) << 16)
                    | ((hmac[offset + 2] & 0xFF) << 8)
                    | (hmac[offset + 3] & 0xFF);

            StringBuilder code = new StringBuilder(5);
            for (int i = 0; i < 5; i++) {
                code.append(STEAM_CODE_CHARS[fullCode % STEAM_CODE_CHARS.length]);
                fullCode /= STEAM_CODE_CHARS.length;
            }
            return code.toString();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot generate SteamGuard code");
        }
    }

    @PostMapping("/trade-token-by-login")
    public java.util.Map<String, Object> saveTradeTokenByLogin(
            @RequestParam("profileLogin") String profileLogin,
            @RequestParam("tradeToken") String tradeToken
    ) {
        UserEntity user = currentUser();
        if (profileLogin == null || profileLogin.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profileLogin required");
        }

        String token = normalizeTradeToken(tradeToken);
        ProfileEntity profile = profileRepo.findReadableByLoginAndUserId(profileLogin, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        SteamGuardEntity guard = sgRepo.findByProfile_ProfileId(profile.getProfileId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Upload the STASH maFile before saving a trade token"));

        guard.setTradeToken(token);
        sgRepo.saveAndFlush(guard);
        return java.util.Map.of("success", true, "message", "Trade token saved");
    }

    private String normalizeTradeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tradeToken required");
        }
        String token = raw.trim();
        int marker = token.indexOf("token=");
        if (marker >= 0) {
            token = token.substring(marker + 6);
            int separator = token.indexOf('&');
            if (separator >= 0) token = token.substring(0, separator);
        }
        if (!token.matches("[A-Za-z0-9_-]{4,128}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Steam trade token");
        }
        return token;
    }}
