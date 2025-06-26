package io.cloudtrust.keycloak.credential;

/**
 * Provider factory for the Salted SHA1 password hash algorithm.
 */
public class Ssha1PasswordHashProviderFactory extends AbstractSshaPasswordHashProviderFactory<Ssha1PasswordHashProvider> {
    public static final String ID = "ssha1";

    @Override
    public Ssha1PasswordHashProvider createProvider() {
        return new Ssha1PasswordHashProvider();
    }

    @Override
    public String getId() {
        return ID;
    }
}
