package steam.vm.controller;

import org.springframework.web.bind.annotation.*;
import steam.vm.dto.UserMapper;
import steam.vm.repo.UserRepository;
import steam.vm.dto.UserDto;


import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserRepository repo;

    public UserApiController(UserRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<UserDto> allUsers() {
        return repo.findAll().stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public UserDto userById(@PathVariable Long id) {
        var user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        return UserMapper.toDto(user);
    }
}
