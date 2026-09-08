package ro.editii.scriptorium.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ro.editii.scriptorium.dao.AppUserRepository;
import ro.editii.scriptorium.model.AppUser;

/**
 * Migrates the single admin account that used to live in SecurityConfig's
 * InMemoryUserDetailsManager into a real, persisted AppUser row - runs once
 * per DB (no-op if the account already exists), so upgrading doesn't lock
 * the existing admin out.
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class AdminUserSeeder {

    final AppUserRepository appUserRepository;
    final PasswordEncoder passwordEncoder;

    @Value("${admin.username:petru}")
    String adminUsername;

    // Same default this app already had hardcoded in SecurityConfig before
    // this migration - not a new exposure, just relocated. Override via
    // ADMIN_PASSWORD once deployed anywhere that default matters.
    @Value("${admin.password:xilofon}")
    String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void seedAdminIfMissing() {
        if (this.appUserRepository.existsByUsername(this.adminUsername))
            return;

        final AppUser admin = AppUser.builder()
                .username(this.adminUsername)
                .passwordHash(this.passwordEncoder.encode(this.adminPassword))
                .role(AppUser.Role.ADMIN)
                .build();
        this.appUserRepository.save(admin);
        log.info("Seeded admin AppUser '{}'", this.adminUsername);
    }
}
