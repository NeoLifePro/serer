package steam.vm.dto;

public class AuthDtos {

    public static class RegisterRequest {
        public String username;
        public String email;
        public String password;
        public String inviteCode;
    }

    public static class LoginRequest {
        public String login;     // username OR email
        public String password;
    }

    public static class AuthResponse {
        public Long id;
        public String username;
        public String email;
        public String role;
        public String token;

        public AuthResponse(Long id, String username, String email, String role, String token) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.role = role;
            this.token = token;
        }
    }
}
