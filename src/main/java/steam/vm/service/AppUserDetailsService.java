package steam.vm.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import steam.vm.entity.UserEntity;
import steam.vm.repo.UserRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    public AppUserDetailsService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        UserEntity user = userRepo.findReadableByUsername(login)
                .orElseGet(() -> userRepo.findReadableByEmail(login)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + login)));

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(user.getRole())
                .build();
    }
}
