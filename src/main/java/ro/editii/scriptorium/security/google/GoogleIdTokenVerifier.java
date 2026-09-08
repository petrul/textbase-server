package ro.editii.scriptorium.security.google;

/**
 * Verifies a Google ID token (the "credential" a One Tap widget posts - see
 * GoogleAuthController) and returns its claims, or throws if it isn't a
 * genuine, current, correctly-audienced Google token. A separate interface
 * (real impl: GoogleTokenInfoVerifier) so tests can substitute a fake
 * instead of calling Google - same reasoning as this codebase's existing
 * Embedder/FakeEmbedderTestConfig split.
 */
public interface GoogleIdTokenVerifier {
    GoogleClaims verify(String idToken);
}
