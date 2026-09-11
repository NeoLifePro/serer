package steam.vm.controller.dashboard;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import steam.vm.dto.ProfileDtos.CreateProfileRequest;
import steam.vm.dto.ProfileDtos.ProfileResponse;
import steam.vm.entity.ProfileEntity;
import steam.vm.entity.UserEntity;
import steam.vm.repo.ProfileRepository;
import steam.vm.repo.UserRepository;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileRepository profileRepo;
    private final UserRepository userRepo;

    public ProfileController(ProfileRepository profileRepo, UserRepository userRepo) {
        this.profileRepo = profileRepo;
        this.userRepo = userRepo;
    }

    @PostMapping
    public ProfileResponse create(@RequestBody CreateProfileRequest req) {

        // user из сессии
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }

        UserEntity user = userRepo.findReadableByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        // валидация
        if (req.login == null || req.login.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "login required");
        if (req.paroleHash == null || req.paroleHash.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paroleHash required");
        if (req.email == null || req.email.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email required");

        ProfileEntity p = new ProfileEntity();
        p.setUser(user);
        p.setProfileType(steam.vm.entity.ProfileType.FARM);
        p.setLogin(req.login);
        p.setParoleHash(req.paroleHash);
        p.setEmail(req.email);
        p.setFarmed(req.farmed != null && req.farmed == 1);
        p.setBlocked(req.blocked != null && req.blocked == 1);

        p = profileRepo.save(p);

        return new ProfileResponse(
                p.getProfileId(),
                user.getId(),
                p.getLogin(),
                p.getEmail(),
                p.isFarmed(),
                p.isBlocked()
        );
    }
}
