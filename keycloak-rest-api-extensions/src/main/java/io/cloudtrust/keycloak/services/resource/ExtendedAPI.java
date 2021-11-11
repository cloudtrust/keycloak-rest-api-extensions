package io.cloudtrust.keycloak.services.resource;

import io.cloudtrust.keycloak.services.resource.api.ApiConfig;
import io.cloudtrust.keycloak.services.resource.api.ApiRoot;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class ExtendedAPI implements RealmResourceProvider {

    private KeycloakSession session;
    private ApiConfig apiConfig;

    public ExtendedAPI(KeycloakSession session, ApiConfig apiConfig) {
        this.session = session;
        this.apiConfig = apiConfig;
    }

    @Override
    public Object getResource() {
        return new ApiRoot(session, apiConfig);
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
