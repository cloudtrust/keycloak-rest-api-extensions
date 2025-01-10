package io.cloudtrust.keycloak.credential;

import org.keycloak.Config;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Provider factory for the Salted SHA256 password hash algorithm.
 */
public class Ssha256PasswordHashProviderFactory implements PasswordHashProviderFactory {
    public static final String ID = "ssha256";

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new Ssha256PasswordHashProvider();
    }

    @Override
    public void init(Config.Scope config) {
        // Nothing to do
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public String getId() {
        return ID;
    }
}
