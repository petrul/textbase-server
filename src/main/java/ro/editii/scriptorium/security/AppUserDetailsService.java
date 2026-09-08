package ro.editii.scriptorium.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ro.editii.scriptorium.dao.AppUserRepository;
import ro.editii.scriptorium.model.AppUser;

/**
 * Backs Spring Security's authentication (both httpBasic and formLogin, see
 * SecurityConfig) with real, persisted AppUser rows instead of the single
 * hardcoded InMemoryUserDetailsManager account this replaced.
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        final AppUser user = this.appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("no such user: " + username));

        // A Google-only account (see GoogleAuthService) has no local
        // password at all - not loadable for httpBasic/formLogin, only
        // reachable through POST /api/auth/google.
        if (user.getPasswordHash() == null)
            throw new UsernameNotFoundException("'" + username + "' only signs in via Google");

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}
