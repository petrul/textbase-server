package ro.editii.scriptorium.security.google

/**
 * Test double for GoogleIdTokenVerifier - treats the "token" as literally
 * "sub|email|name", so tests can construct arbitrary Google identities
 * without needing a real Google credential or network call. See
 * FakeGoogleAuthTestConfig for how this replaces the real
 * GoogleTokenInfoVerifier bean in tests.
 */
class FakeGoogleIdTokenVerifier implements GoogleIdTokenVerifier {

    static final String INVALID_TOKEN = "__invalid__"

    @Override
    GoogleClaims verify(String idToken) {
        if (idToken == null || idToken == INVALID_TOKEN)
            throw new IllegalArgumentException("invalid Google credential")

        final parts = idToken.split('\\|', 3)
        return new GoogleClaims(parts[0], parts.length > 1 ? parts[1] : null, parts.length > 2 ? parts[2] : null, true)
    }

    static String fakeToken(String sub, String email = null, String name = null) {
        return [sub, email, name].join('|')
    }
}
