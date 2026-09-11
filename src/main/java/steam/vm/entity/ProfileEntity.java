package steam.vm.entity;

import jakarta.persistence.*;
import steam.vm.config.crypto.EncryptedStringConverter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profile")
public class ProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_type", nullable = false)
    private ProfileType profileType = ProfileType.FARM;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String login;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "parole_hash", nullable = false, length = 512)
    private String paroleHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String email;

    @Column(nullable = false)
    private boolean farmed = false;

    @Column(nullable = false)
    private boolean blocked = false;

    @OneToOne(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private SteamGuardEntity steamguard;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VmPanelEntity> vms = new ArrayList<>();

    public List<ItemEntity> getItems() {
        return items;
    }

    public void setItems(List<ItemEntity> items) {
        this.items = items;
    }

    public List<VmPanelEntity> getVms() {
        return vms;
    }

    public void setVms(List<VmPanelEntity> vms) {
        this.vms = vms;
    }

    public SteamGuardEntity getSteamguard() {
        return steamguard;
    }

    public void setSteamguard(SteamGuardEntity steamguard) {
        this.steamguard = steamguard;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isFarmed() {
        return farmed;
    }

    public void setFarmed(boolean farmed) {
        this.farmed = farmed;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getParoleHash() {
        return paroleHash;
    }

    public void setParoleHash(String paroleHash) {
        this.paroleHash = paroleHash;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public ProfileType getProfileType() { return profileType; }

    public void setProfileType(ProfileType profileType) { this.profileType = profileType; }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemEntity> items = new ArrayList<>();

    // getters/setters
}
