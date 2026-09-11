package steam.vm.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import steam.vm.entity.UserEntity;
import steam.vm.repo.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthTokenService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final SecureRandom random = new SecureRandom();

    public AuthTokenService(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    public String createToken(UserEntity user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("""
                INSERT INTO auth_token (user_id, token_hash, token_type, expires_time)
                VALUES (?, ?, 'refresh', DATE_ADD(NOW(), INTERVAL 10 MINUTE))
                """, user.getId(), hash(token));
        return token;
    }

    public Optional<UserEntity> verify(String token) {
        if (token == null || token.length() != 43) return Optional.empty();
        return jdbc.queryForList("""
                SELECT user_id FROM auth_token
                WHERE token_hash = ? AND token_type = 'refresh'
                  AND revoked = 0 AND expires_time > NOW()
                """, Long.class, hash(token)).stream().findFirst()
                .flatMap(users::findById)
                .filter(user -> !"banned".equalsIgnoreCase(user.getRole()));
    }

    public boolean refresh(String token) {
    
        return jdbc.update("""
                UPDATE auth_token
                SET expires_time = DATE_ADD(NOW(), INTERVAL 10 MINUTE), last_used_time = NOW()
                WHERE token_hash = ? AND token_type = 'refresh'
                  AND revoked = 0 AND expires_time > NOW()
                """, hash(token)) == 1;
    }

    public void revoke(String token) {
        jdbc.update("UPDATE auth_token SET revoked = 1 WHERE token_hash = ?", hash(token));
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
