package steam.vm.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import steam.vm.entity.UserEntity;
import steam.vm.repo.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthTokenServiceTests {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AuthTokenService service = new AuthTokenService(jdbc, users);

    @Test
    void loginCreatesUniqueTokensAndStoresOnlyHash() throws Exception {
        UserEntity user = new UserEntity();
        user.setId(7L);
        String token = service.createToken(user);
        assertEquals(43, token.length());
        assertNotEquals(token, service.createToken(user));
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        verify(jdbc).update(contains("INTERVAL 10 MINUTE"), eq(7L), eq(hash));
    }

    @Test
    void malformedAndOldJwtTokensAreRejectedWithoutDatabaseAccess() {
        assertTrue(service.verify(null).isEmpty());
        assertTrue(service.verify("old.jwt.token").isEmpty());
        verifyNoInteractions(jdbc, users);
    }

    @Test
    void missingExpiredOrRevokedTokenCannotAuthenticate() {
        when(jdbc.queryForList(anyString(), eq(Long.class), anyString())).thenReturn(List.of());
        assertTrue(service.verify("a".repeat(43)).isEmpty());
        verifyNoInteractions(users);
    }

    @Test
    void currentUserRoleIsCheckedIncludingBans() {
        UserEntity user = new UserEntity();
        user.setRole("banned");
        when(jdbc.queryForList(anyString(), eq(Long.class), anyString())).thenReturn(List.of(7L));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        assertTrue(service.verify("a".repeat(43)).isEmpty());
        user.setRole("user");
        assertEquals(user, service.verify("a".repeat(43)).orElseThrow());
    }

    @Test
    void refreshFailsWhenNoActiveSessionWasUpdated() {
        when(jdbc.update(anyString(), anyString())).thenReturn(0, 1);
        assertFalse(service.refresh("a".repeat(43)));
        assertTrue(service.refresh("a".repeat(43)));
    }
}
