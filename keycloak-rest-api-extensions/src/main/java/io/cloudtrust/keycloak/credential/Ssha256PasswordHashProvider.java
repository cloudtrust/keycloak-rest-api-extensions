package io.cloudtrust.keycloak.credential;

/**
 * Implementation of the Salted SHA256 password hash algorithm.
 */
public class Ssha256PasswordHashProvider extends AbstractSshaPasswordHashProvider {
    private static final String SSHA256_PREFIX = "{SSHA256}";
    private static final int SSHA256_OUTPUT_BYTE_LENGTH = 32;

    public Ssha256PasswordHashProvider() {
        super("SHA-256", SSHA256_PREFIX, SSHA256_OUTPUT_BYTE_LENGTH, Ssha256PasswordHashProviderFactory.ID);
    }
}
