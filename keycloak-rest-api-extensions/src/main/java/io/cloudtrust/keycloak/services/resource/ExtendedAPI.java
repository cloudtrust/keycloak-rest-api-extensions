package io.cloudtrust.keycloak.services.resource;

import io.cloudtrust.keycloak.services.resource.api.ApiRoot;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class ExtendedAPI implements RealmResourceProvider {

    private KeycloakSession session;

    public ExtendedAPI(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new ApiRoot(session);
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
