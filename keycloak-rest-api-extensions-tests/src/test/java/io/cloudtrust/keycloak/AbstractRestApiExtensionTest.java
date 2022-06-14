package io.cloudtrust.keycloak;

import io.cloudtrust.keycloak.test.AbstractInKeycloakTest;
import io.cloudtrust.keycloak.test.init.InjectionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.IOException;
import java.util.stream.Stream;

public abstract class AbstractRestApiExtensionTest extends AbstractInKeycloakTest {
    @BeforeEach
    public void setupTest() throws IOException, InjectionException {
        this.injectComponents();
        this.createRealm("/testrealm.json");
        this.api().initToken();
        // Clean events
        this.events().activate("test");
        this.events().clear();

        createDummyRealm("dummy1", "invalid-theme");
        createDummyRealm("dummy2", "keycloak");
    }

    @AfterEach
    public void deleteDummyRealms() {
        Stream.of("dummy1", "dummy2").forEach(this::deleteRealm);
    }

    private void createDummyRealm(String realm, String theme) {
        this.deleteRealm(realm);
        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setId(realm);
        realmRepresentation.setRealm(realm);
        realmRepresentation.setEnabled(true);
        realmRepresentation.setEmailTheme(theme);
        this.getKeycloakAdminClient().realms().create(realmRepresentation);
    }
}
