package steam.vm.entity;

import jakarta.persistence.*;
import steam.vm.config.crypto.EncryptedStringConverter;

@Entity
@Table(name = "steamguard")
public class SteamGuardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guard_id")
    private Long guardId;

    @OneToOne
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private ProfileEntity profile;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "steamid64", nullable = false, unique = true, length = 512)
    private String steamid64;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "shared_secret", nullable = false, length = 512)
    private String sharedSecret;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "identity_secret", nullable = false, length = 512)
    private String identitySecret;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "device_id", length = 512)
    private String deviceId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "trade_token", length = 1024)
    private String tradeToken;

    public Long getGuardId() { return guardId; }
    public void setGuardId(Long guardId) { this.guardId = guardId; }

    public ProfileEntity getProfile() { return profile; }
    public void setProfile(ProfileEntity profile) { this.profile = profile; }

    public String getSteamid64() { return steamid64; }
    public void setSteamid64(String steamid64) { this.steamid64 = steamid64; }

    public String getSharedSecret() { return sharedSecret; }
    public void setSharedSecret(String sharedSecret) { this.sharedSecret = sharedSecret; }

    public String getIdentitySecret() { return identitySecret; }
    public void setIdentitySecret(String identitySecret) { this.identitySecret = identitySecret; }

    public String getTradeToken() { return tradeToken; }
    public void setTradeToken(String tradeToken) { this.tradeToken = tradeToken; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
