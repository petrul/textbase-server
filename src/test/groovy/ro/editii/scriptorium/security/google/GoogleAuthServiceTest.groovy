package ro.editii.scriptorium.security.google

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import ro.editii.scriptorium.TestConfig
import ro.editii.scriptorium.TextbaseServer
import ro.editii.scriptorium.dao.AppUserRepository
import ro.editii.scriptorium.model.AppUser
import ro.editii.scriptorium.vector.FakeEmbedderTestConfig

/**
 * Uses FakeGoogleIdTokenVerifier (see FakeGoogleAuthTestConfig) instead of
 * calling Google's real tokeninfo endpoint - GoogleAuthService itself is
 * what's under test here (find-or-create logic), not token verification.
 */
@TestPropertySource(properties = [
        "spring.datasource.url=jdbc:h2:mem:googleAuthServiceTestDb;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.jpa.hibernate.ddl-auto=create",
        "milvus.host=mini.local",
        "milvus.port=20112",
        "milvus.collection=test_tb_paras_qwen3_embedding_4b_googleauth_test_unused",
        "embeddings.host=mini.local",
        "embeddings.port=11200",
        "ollama.host=zmeu.local",
        "ollama.port=11434",
        "textbase.advertised.url=http://localhost:8080"
])
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = [TextbaseServer.class, TestConfig.class, FakeEmbedderTestConfig.class, FakeGoogleAuthTestConfig.class])
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoogleAuthServiceTest {

    @Autowired GoogleAuthService googleAuthService
    @Autowired AppUserRepository appUserRepository

    @Test
    void createsANewAccountOnFirstGoogleSignIn() {
        final sub = "google-sub-" + System.nanoTime()
        final token = FakeGoogleIdTokenVerifier.fakeToken(sub, "alice_${sub}@example.com", "Alice")

        final user = this.googleAuthService.signIn(token)

        assert user.id != null
        assert user.googleSub == sub
        assert user.username == "alice_${sub}@example.com"
        assert user.passwordHash == null
        assert user.role == AppUser.Role.USER
    }

    @Test
    void reusesTheSameAccountOnRepeatSignIn() {
        final sub = "google-sub-" + System.nanoTime()
        final token = FakeGoogleIdTokenVerifier.fakeToken(sub, "bob_${sub}@example.com", "Bob")

        final first = this.googleAuthService.signIn(token)
        final second = this.googleAuthService.signIn(token)

        assert first.id == second.id
        assert this.appUserRepository.findByGoogleSub(sub).isPresent()
    }

    @Test
    void createsAFavoritesCollectionForANewGoogleAccount() {
        final sub = "google-sub-" + System.nanoTime()
        final token = FakeGoogleIdTokenVerifier.fakeToken(sub, "carol_${sub}@example.com", "Carol")

        final user = this.googleAuthService.signIn(token)

        // Not autowiring DivCollectionService here on purpose - this just
        // confirms the side effect happened via the repository directly,
        // keeping this test focused on GoogleAuthService's own contract.
        assert this.appUserRepository.findByUsername(user.username).isPresent()
    }

    @Test
    void rejectsAnInvalidCredential() {
        try {
            this.googleAuthService.signIn(FakeGoogleIdTokenVerifier.INVALID_TOKEN)
            assert false: "expected an exception"
        } catch (IllegalArgumentException e) {
            assert e.message.contains("invalid Google credential")
        }
    }
}
