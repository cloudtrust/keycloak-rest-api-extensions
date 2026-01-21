package io.cloudtrust.keycloak.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

import java.io.IOException;

public class AbstractRealmConfig implements RealmConfig {
    private final String filename;

    protected AbstractRealmConfig(String filename) {
        this.filename = filename;
    }

    protected void customizeRealm(RealmRepresentation realmRepresentation) {
        // Default implementation does nothing. Subclasses may override to customize the realm.
    }

    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realmConfigBuilder) {
        try {
            var inputStream = getClass().getResourceAsStream(filename);
            var realmRepresentation = new ObjectMapper().readValue(inputStream, RealmRepresentation.class);
            customizeRealm(realmRepresentation);
            return RealmConfigBuilder.update(realmRepresentation);
        } catch (IOException e) {
            Logger.getLogger(AbstractRealmConfig.class).error("Configuration failed", e);
        }
        return realmConfigBuilder;
    }
}
