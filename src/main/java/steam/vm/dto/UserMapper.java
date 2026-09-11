package steam.vm.dto;

import steam.vm.entity.UserEntity;

public class UserMapper {
    private UserMapper() {}

    public static UserDto toDto(UserEntity u) {
        return new UserDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole());
    }
}
