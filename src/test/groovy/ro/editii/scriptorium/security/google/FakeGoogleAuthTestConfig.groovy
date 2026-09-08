package ro.editii.scriptorium.security.google

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class FakeGoogleAuthTestConfig {
    @Bean
    @Primary
    GoogleIdTokenVerifier googleIdTokenVerifier() {
        return new FakeGoogleIdTokenVerifier()
    }
}
