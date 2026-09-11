package steam.vm.dto;

public class ProfileDtos {

    public static class CreateProfileRequest {
        public String login;
        public String paroleHash;
        public String email;
        public Integer farmed;
        public Integer blocked;
    }

    public static class ProfileResponse {
        public Long profileId;
        public Long userId;
        public String login;
        public String email;
        public boolean farmed;
        public boolean blocked;

        public ProfileResponse(Long profileId, Long userId, String login, String email, boolean farmed, boolean blocked) {
            this.profileId = profileId;
            this.userId = userId;
            this.login = login;
            this.email = email;
            this.farmed = farmed;
            this.blocked = blocked;
        }
    }
}
