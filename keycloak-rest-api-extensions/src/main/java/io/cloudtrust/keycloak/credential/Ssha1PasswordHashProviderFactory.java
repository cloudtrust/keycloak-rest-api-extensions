package io.cloudtrust.keycloak.credential;

import org.keycloak.Config;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Provider factory for the Salted SHA1 password hash algorithm.
 */
public class Ssha1PasswordHashProviderFactory implements PasswordHashProviderFactory {
    public static final String ID = "ssha1";

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new Ssha1PasswordHashProvider();
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
