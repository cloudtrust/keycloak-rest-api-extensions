package io.cloudtrust.keycloak;

import io.cloudtrust.keycloak.config.TestRealmConfig;
import io.cloudtrust.keycloak.test.AbstractKeycloakTest;
import io.cloudtrust.keycloak.test.ExtensionApi;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.realm.ManagedRealm;

public abstract class AbstractRestApiExtensionTest extends AbstractKeycloakTest {
    @InjectAdminClient
    protected Keycloak keycloak;

    @InjectRealm(config = TestRealmConfig.class, ref = "test")
    protected ManagedRealm testRealm;

    @InjectRealm(ref = "dummy1")
    protected ManagedRealm dummyRealm1;

    @InjectRealm(ref = "dummy2")
    protected ManagedRealm dummyRealm2;

    @InjectEvents(realmRef = "test")
    protected Events events;

    @InjectAdminEvents(realmRef = "test")
    protected AdminEvents adminEvents;

    @BeforeEach
    void setupTest() {
        dummyRealm1.getCreatedRepresentation().setEmailTheme("invalid-theme");
        dummyRealm1.getCreatedRepresentation().setId("dummy1");

        dummyRealm2.getCreatedRepresentation().setEmailTheme("keycloak");
        dummyRealm2.getCreatedRepresentation().setId("dummy2");

        // Clean events
        events.clear();
        adminEvents.clear();
    }
}
