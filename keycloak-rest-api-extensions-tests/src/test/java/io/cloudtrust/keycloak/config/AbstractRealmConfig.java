package io.cloudtrust.keycloak.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

public class AbstractRealmConfig implements RealmConfig {

    private final String filename;

    protected AbstractRealmConfig(String filename) {
        this.filename = filename;
    }

    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realmConfigBuilder) {
        try {
            RealmRepresentation realmRepresentation = new ObjectMapper().readValue(
                    getClass().getResourceAsStream(filename),
                    RealmRepresentation.class
            );
            return RealmConfigBuilder.update(realmRepresentation);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return realmConfigBuilder;
    }
}
