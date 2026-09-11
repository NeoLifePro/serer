package steam.vm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import steam.vm.service.AuthTokenService;
import steam.vm.dto.AuthDtos.AuthResponse;
import steam.vm.dto.AuthDtos.LoginRequest;
import steam.vm.dto.AuthDtos.RegisterRequest;
import steam.vm.dto.UserDto;
import steam.vm.dto.UserMapper;
import steam.vm.entity.InviteCodeEntity;
import steam.vm.entity.UserEntity;
import steam.vm.repo.InviteCodeRepository;
import steam.vm.repo.UserRepository;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepo;
    private final InviteCodeRepository inviteCodeRepo;
    private final AuthTokenService tokenService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(
            UserRepository userRepo,
            InviteCodeRepository inviteCodeRepo,
            AuthTokenService tokenService
    ) {
        this.userRepo = userRepo;
        this.inviteCodeRepo = inviteCodeRepo;
        this.tokenService = tokenService;
    }
    @GetMapping("/me")
    public UserDto me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }

        UserEntity user = userRepo.findReadableByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        if (isBanned(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "account banned");
        }

        return UserMapper.toDto(user);
    }
    @PostMapping("/register")
    @Transactional
    public AuthResponse register(@RequestBody RegisterRequest req) {
        if (req.username == null || req.username.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username required");
        if (req.email == null || req.email.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email required");
        if (req.password == null || req.password.length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password min 6");
        if (req.inviteCode == null || req.inviteCode.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid invite code");

        String inviteCodeValue = req.inviteCode.trim();
        InviteCodeEntity inviteCode = inviteCodeRepo.findByCode(inviteCodeValue)
                .filter(code -> code.getActivatedByUser() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid invite code"));

        if (userRepo.findReadableByUsername(req.username).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        if (userRepo.findReadableByEmail(req.email).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");

        UserEntity u = new UserEntity();
        u.setUsername(req.username);
        u.setEmail(req.email);
        u.setPasswordHash(encoder.encode(req.password));
        u.setRole("user");
        u.setRegisteredTime(LocalDateTime.now());

        u = userRepo.save(u);
        inviteCode.setActivatedByUser(u);
        inviteCode.setActivatedAt(LocalDateTime.now());
        inviteCodeRepo.save(inviteCode);

        return authResponse(u);
    }

    @PostMapping("/login")
    @Transactional(timeout = 8)
    public AuthResponse login(@RequestBody LoginRequest req) {
        if (req.login == null || req.login.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "login required");
        if (req.password == null || req.password.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password required");

        log.info("Login attempt for {}", req.login);
        long start = System.currentTimeMillis();

        UserEntity user = userRepo.findReadableByUsernameOrEmail(req.login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "bad credentials"));

        log.info("Login user lookup for {} took {} ms", req.login, System.currentTimeMillis() - start);

        if (!encoder.matches(req.password, user.getPasswordHash())) {
            log.info("Login failed for {}: bad password", req.login);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "bad credentials");
        }

        if (isBanned(user)) {
            log.info("Login failed for {}: banned account", req.login);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "account banned");
        }

        log.info("Login password check for {} took {} ms", req.login, System.currentTimeMillis() - start);

        user.setLastConnection(LocalDateTime.now());
        userRepo.save(user);

        log.info("Login success for {} took {} ms", user.getUsername(), System.currentTimeMillis() - start);

        return authResponse(user);
    }

    private AuthResponse authResponse(UserEntity user) {
        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                tokenService.createToken(user)
        );
    }

    @PostMapping("/refresh")
    public void refresh(@RequestHeader(value = "Authorization", defaultValue = "") String header) {
        String token = bearerToken(header);
        if (tokenService.verify(token).isEmpty() || !tokenService.refresh(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "session expired");
        }
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", defaultValue = "") String header) {
        tokenService.revoke(bearerToken(header));
    }

    private String bearerToken(String header) {
        return header.startsWith("Bearer ") ? header.substring(7) : "";
    }

    private boolean isBanned(UserEntity user) {
        return "banned".equalsIgnoreCase(user.getRole());
    }
}
