package io.cloudtrust.keycloak.credential;

import io.cloudtrust.keycloak.config.ServerConfig;
import io.cloudtrust.keycloak.config.realm.TestRealm;
import io.cloudtrust.keycloak.test.AbstractKeycloakTest;
import io.cloudtrust.keycloak.test.ExtensionApi;
import io.cloudtrust.keycloak.test.util.OidcTokenProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.slf4j.LoggerFactory;

import static io.cloudtrust.keycloak.credential.CtPasswordCreatedAdminEventMatcher.isPasswordCreatedAdminEvent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest(config = ServerConfig.class)
class CtPasswordCredentialKeycloakProviderTest extends AbstractKeycloakTest {

    private static final Logger logger = Logger.getLogger(CtPasswordCredentialProviderTest.class);
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CtPasswordCredentialKeycloakProviderTest.class);

    @InjectAdminClient
    Keycloak keycloak;

    @InjectRealm(config = TestRealm.class)
    ManagedRealm testRealm;

    @InjectAdminEvents
    AdminEvents adminEvents;

    private AdminEventRepresentation pollIt() {
        return adminEvents.poll();
    }

    // TODO to put in parent cloudtrust public
    private ExtensionApi api(Keycloak keycloak, ManagedRealm realm) {
        String baseUrl = realm.getBaseUrl();
        baseUrl = baseUrl.substring(0, baseUrl.indexOf("/realms/"));
        return new ExtensionApi(baseUrl, () -> keycloak.tokenManager().getAccessTokenString());
    }

    // TODO to put in parent cloudtrust public
    public OidcTokenProvider createOidcTokenProvider(ManagedRealm realm, String username, String password) {
        return createOidcTokenProviderTest(realm, username, password);
    }

    public OidcTokenProvider createOidcTokenProviderTest(ManagedRealm realm, String username, String password) {
        log.info("base url {}", realm.getBaseUrl());
        log.info("realm name {}", realm.getName());
        return new OidcTokenProvider(realm.getBaseUrl(), "/protocol/openid-connect/token", username, password);

        // http://localhost:8080/realms/test/protocol/openid-connect/token
    }

    //    ---------------------------------------------------------------------------------------------------------------------------------------------------
    @Test
    void adminResetPassword() throws Exception {
        UserRepresentation user = testRealm.admin().users().search("test-user@localhost").getFirst();

        Map<String, Object> body = Map.of(
                "type", "password",
                "value", "P@ssw0rd",
                "temporary", false);

        ExtensionApi apiResource = api(keycloak, testRealm);
        apiResource.callJSON("PUT", "/admin/realms/test/users/" + user.getId() + "/reset-password", body);
        AdminEventRepresentation adminEventRepresentation = pollIt();
        assertThat(adminEventRepresentation, isPasswordCreatedAdminEvent());
        // put a afterEach to check adminEvent is null and no event after this action
    }

    @Test
    void selfResetPassword() throws IOException, URISyntaxException {
        String clientId = "account";

        updateRealmClient(
                testRealm,
                clientId,
                c -> {
                    c.setDirectAccessGrantsEnabled(Boolean.TRUE);
                });

//        for testing purposes
//        log.info("sleep");
//        sleep(Duration.ofMinutes(3));

        OidcTokenProvider tokenProvider = createOidcTokenProvider(testRealm, clientId, "");
        String currentPassword = "P@55w0rd!";
        String token = tokenProvider.getAccessToken("test-user@localhost", currentPassword);
        log.info("token {}", token);
        assertThat(token, is(not(nullValue())));
        testRealm.admin().getEvents().clear();

        Map<String, Object> body = Map.of(
                "currentPassword", currentPassword,
                "newPassword", "P@ssw0rd",
                "confirmation", "P@ssw0rd");

        ExtensionApi apiResource = api(keycloak, testRealm);
        apiResource.setToken(token);
        log.info("token {}", token);
        // TODO fix path
        apiResource.callJSON("POST", "/realms/master/api/account/realms/test/credentials/password", body);
//        logger.infof("Update password response: %s", resp);

//        assertThat(testRealm.admin().getEvents().getFirst(), isPasswordCreatedEvent());
    }

}
