package io.cloudtrust.keycloak.credential;

import io.cloudtrust.keycloak.config.AccountOauthClientConfig;
import io.cloudtrust.keycloak.config.ServerConfig;
import io.cloudtrust.keycloak.config.TestRealmConfig;
import io.cloudtrust.keycloak.test.AbstractKeycloakTest;
import io.cloudtrust.keycloak.test.ExtensionApi;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;

import static io.cloudtrust.keycloak.credential.CtLoginEventMatcher.isLoginEvent;
import static io.cloudtrust.keycloak.credential.CtPasswordCreatedEventMatcher.isPasswordCreatedEvent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest(config = ServerConfig.class)
class CtPasswordCredentialProviderTest extends AbstractKeycloakTest {

    private static final Logger logger = Logger.getLogger(CtPasswordCredentialProviderTest.class);

    @InjectAdminClient
    Keycloak keycloak;

    @InjectRealm(config = TestRealmConfig.class, ref = "test")
    ManagedRealm testRealm;

    @InjectEvents(realmRef = "test")
    Events events;

    @InjectOAuthClient(realmRef = "test", config = AccountOauthClientConfig.class)
    OAuthClient oauthClient;

    private ExtensionApi api(Keycloak keycloak, ManagedRealm realm) {
        String baseUrl = realm.getBaseUrl();
        baseUrl = baseUrl.substring(0, baseUrl.indexOf("/realms/"));
        return new ExtensionApi(baseUrl, () -> keycloak.tokenManager().getAccessTokenString());
    }

    @Test
    void selfResetPassword() throws IOException, URISyntaxException {
        String clientId = "account";
        String username = "test-user@localhost";
        String currentPassword = "P@55w0rd!";

        updateRealmClient(testRealm, clientId, c -> c.setDirectAccessGrantsEnabled(Boolean.TRUE));

        String token = oauthClient.doPasswordGrantRequest(username, currentPassword).getAccessToken();

        assertThat(token, is(not(nullValue())));

        testRealm.admin().getEvents().clear();

        Map<String, Object> body = Map.of(
                "currentPassword", currentPassword,
                "newPassword", "P@ssw0rd",
                "confirmation", "P@ssw0rd");

        ExtensionApi apiResource = api(keycloak, testRealm);
        apiResource.setToken(token);

        assertThat(events.poll(), isLoginEvent());

        String resp = apiResource.callJSON("POST", "/realms/master/api/account/realms/test/credentials/password", body);
        logger.infof("Update password response: %s", resp);

        assertThat(events.poll(), isPasswordCreatedEvent());
    }

}
