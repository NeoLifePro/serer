package steam.vm.dto;

import java.time.LocalDateTime;

public class AccountRow {

    public Long profileId;
    public String login;
    public String email;
    public String paroleHash;

    public boolean farmed;
    public boolean blocked;
    public boolean hasSteamGuard;
    public Double earnedTotal;

    public String vmName;
    public LocalDateTime lastTimeStarted;

    public AccountRow(Long profileId,
                      String login,
                      String email,
                      String paroleHash,
                      boolean farmed,
                      boolean blocked,
                      boolean hasSteamGuard,
                      Double earnedTotal,
                      String vmName,
                      LocalDateTime lastTimeStarted) {

        this.profileId = profileId;
        this.login = login;
        this.email = email;
        this.paroleHash = paroleHash;
        this.farmed = farmed;
        this.blocked = blocked;
        this.hasSteamGuard = hasSteamGuard;
        this.earnedTotal = earnedTotal;
        this.vmName = vmName;
        this.lastTimeStarted = lastTimeStarted;
    }
}
