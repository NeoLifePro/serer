package steam.vm.dto;

import java.time.LocalDateTime;
import java.util.List;

public class VmPanelDtos {

    public static class VmPanelAssignRequest {
        public String profileLogin;
        public String vmName;
    }

    public static class VmPanelSummaryRow {
        public String vmName;
        public boolean active;
        public int profileCount;
        public List<String> profileLogins;
        public LocalDateTime lastTimeStarted;

        public VmPanelSummaryRow(String vmName, boolean active, int profileCount, List<String> profileLogins, LocalDateTime lastTimeStarted) {
            this.vmName = vmName;
            this.active = active;
            this.profileCount = profileCount;
            this.profileLogins = profileLogins;
            this.lastTimeStarted = lastTimeStarted;
        }
    }

    public static class MyProfileRow {
        public Long profileId;
        public String login;
        public String email;
        public boolean farmed;
        public boolean blocked;
        public boolean hasSteamGuard;

        public MyProfileRow(Long profileId, String login, String email, boolean farmed, boolean blocked, boolean hasSteamGuard) {
            this.profileId = profileId;
            this.login = login;
            this.email = email;
            this.farmed = farmed;
            this.blocked = blocked;
            this.hasSteamGuard = hasSteamGuard;
        }
    }
}
