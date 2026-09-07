package io.cloudtrust.keycloak.restapi.services.resource;

import io.cloudtrust.keycloak.restapi.services.resource.api.ApiConfig;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

import java.util.concurrent.TimeUnit;

public class ExtendedAPIFactory implements RealmResourceProviderFactory {
    private static final String TERMS_OF_USE_ACCEPTANCE_DELAY_CONFIG_KEY = "termsOfUseAcceptanceDelayDays";

    public static final String ID = "api";

    private final ApiConfig apiConfig = new ApiConfig();

    @Override
    public RealmResourceProvider create(KeycloakSession keycloakSession) {
        return new ExtendedAPI(keycloakSession, apiConfig);
    }

    @Override
    public void init(Config.Scope scope) {
        int delay = getInt(scope, TERMS_OF_USE_ACCEPTANCE_DELAY_CONFIG_KEY, "Terms of use acceptance delay");
        apiConfig.setTermsOfUseAcceptanceDelayMillis(TimeUnit.DAYS.toMillis(delay));
    }

    private static int getInt(Config.Scope config, String configName, String configDesc) {
        try {
            return Integer.parseInt(config.get(configName));
        } catch (NumberFormatException ex) {
            String message = "No " + configDesc + " found in the configuration.";
            throw new IllegalArgumentException(message, ex);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
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
