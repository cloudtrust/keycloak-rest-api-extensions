package io.cloudtrust.keycloak.credential;

/**
 * Implementation of the Salted SHA1 password hash algorithm.
 */
public class Ssha1PasswordHashProvider extends AbstractSshaPasswordHashProvider {
    private static final String SSHA1_PREFIX = "{SSHA}";
    private static final int SSHA1_OUTPUT_BYTE_LENGTH = 20;

    public Ssha1PasswordHashProvider() {
        super("SHA-1", SSHA1_PREFIX, SSHA1_OUTPUT_BYTE_LENGTH, Ssha1PasswordHashProviderFactory.ID);
    }
}
