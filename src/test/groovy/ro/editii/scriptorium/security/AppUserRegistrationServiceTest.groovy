package ro.editii.scriptorium.security

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import ro.editii.scriptorium.collection.DivCollectionService
import ro.editii.scriptorium.dao.AppUserRepository
import ro.editii.scriptorium.model.AppUser

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.*

class AppUserRegistrationServiceTest {

    AppUserRepository repository
    PasswordEncoder passwordEncoder
    DivCollectionService collectionService
    AppUserRegistrationService service

    @BeforeEach
    void setUp() {
        this.repository = mock(AppUserRepository)
        this.passwordEncoder = mock(PasswordEncoder)
        this.collectionService = mock(DivCollectionService)
        this.service = new AppUserRegistrationService(repository, passwordEncoder, collectionService)
    }

    @Test
    void createsAUserWithAHashedPasswordAndFavorites() {
        when(repository.existsByUsername("alice")).thenReturn(false)
        when(passwordEncoder.encode("correcthorsebattery")).thenReturn("encoded-password")
        when(repository.save(any(AppUser))).thenAnswer { invocation ->
            final user = invocation.getArgument(0, AppUser)
            user.id = 7L
            return user
        }

        final user = service.register("alice", "correcthorsebattery")

        assert user.id == 7L
        assert user.username == "alice"
        assert user.passwordHash == "encoded-password"
        assert user.role == AppUser.Role.USER
        verify(collectionService).createFavoritesIfMissing(user)
    }

    @Test
    void rejectsADuplicateUsernameWithoutHashingOrSaving() {
        when(repository.existsByUsername("alice")).thenReturn(true)

        final ex = shouldFail { service.register("alice", "correcthorsebattery") }

        assert ex.message.contains("already taken")
        verifyNoInteractions(passwordEncoder, collectionService)
        verify(repository, never()).save(any(AppUser))
    }

    @Test
    void rejectsATooShortPasswordBeforeConsultingTheRepository() {
        final ex = shouldFail { service.register("alice", "short") }

        assert ex.message.contains("at least")
        verifyNoInteractions(repository, passwordEncoder, collectionService)
    }

    @Test
    void rejectsABlankUsername() {
        final ex = shouldFail { service.register("  ", "correcthorsebattery") }

        assert ex.message.contains("must not be blank")
        verifyNoInteractions(repository, passwordEncoder, collectionService)
    }

    private static Exception shouldFail(Closure closure) {
        try {
            closure.call()
        } catch (Exception e) {
            return e
        }
        throw new AssertionError("expected an exception but none was thrown")
    }
}
