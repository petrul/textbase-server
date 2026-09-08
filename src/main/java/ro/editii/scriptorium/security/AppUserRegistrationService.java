package ro.editii.scriptorium.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.editii.scriptorium.collection.DivCollectionService;
import ro.editii.scriptorium.dao.AppUserRepository;
import ro.editii.scriptorium.model.AppUser;

@Service
@RequiredArgsConstructor
public class AppUserRegistrationService {

    // Real password policy is out of scope for this first slice (see the
    // branch's commit history) - just enough to reject the obviously-wrong
    // cases (empty, trivially short) rather than accept anything at all.
    private static final int MIN_PASSWORD_LENGTH = 8;

    final AppUserRepository appUserRepository;
    final PasswordEncoder passwordEncoder;
    final DivCollectionService divCollectionService;

    @Transactional
    public AppUser register(String username, String password) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("username must not be blank");
        if (username.length() > AppUser.USERNAME_MAX_LENGTH)
            throw new IllegalArgumentException("username too long (max " + AppUser.USERNAME_MAX_LENGTH + " chars)");
        if (password == null || password.length() < MIN_PASSWORD_LENGTH)
            throw new IllegalArgumentException("password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        if (this.appUserRepository.existsByUsername(username))
            throw new IllegalArgumentException("username already taken: " + username);

        final AppUser user = AppUser.builder()
                .username(username)
                .passwordHash(this.passwordEncoder.encode(password))
                .role(AppUser.Role.USER)
                .build();

        this.appUserRepository.save(user);
        this.divCollectionService.createFavoritesIfMissing(user);
        return user;
    }
}
