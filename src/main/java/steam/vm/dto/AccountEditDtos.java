package steam.vm.dto;

public class AccountEditDtos {

    public static class AccountDetailsResponse {
        public Long profileId;
        public String login;
        public String email;
        public String paroleHash;
        public boolean farmed;
        public boolean blocked;

        public boolean hasSteamGuard;

        public String steamid64;
        public String sharedSecret;
        public String identitySecret;
        public String deviceId;
    }

    public static class UpdateAccountRequest {
        public String login;
        public String email;
        public String paroleHash;
        public Boolean farmed;
        public Boolean blocked;

        public Boolean hasSteamGuard;

        public String steamid64;
        public String sharedSecret;
        public String identitySecret;
        public String deviceId;
    }
}