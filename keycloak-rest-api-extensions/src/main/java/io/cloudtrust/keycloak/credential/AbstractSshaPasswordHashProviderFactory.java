package io.cloudtrust.keycloak.credential;

import org.keycloak.Config;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Abstract base class for SSHA password hash provider factories.
 */
public abstract class AbstractSshaPasswordHashProviderFactory<T extends PasswordHashProvider> implements PasswordHashProviderFactory {

    protected abstract T createProvider();

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return createProvider();
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

}
