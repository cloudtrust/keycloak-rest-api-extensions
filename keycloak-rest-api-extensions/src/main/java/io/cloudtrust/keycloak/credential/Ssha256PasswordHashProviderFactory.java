package io.cloudtrust.keycloak.credential;

/**
 * Provider factory for the Salted SHA256 password hash algorithm.
 */
public class Ssha256PasswordHashProviderFactory extends AbstractSshaPasswordHashProviderFactory<Ssha256PasswordHashProvider> {
    public static final String ID = "ssha256";

    @Override
    public Ssha256PasswordHashProvider createProvider() {
        return new Ssha256PasswordHashProvider();
    }

    @Override
    public String getId() {
        return ID;
    }
}
