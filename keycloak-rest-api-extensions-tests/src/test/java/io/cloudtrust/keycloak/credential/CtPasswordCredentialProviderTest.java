package io.cloudtrust.keycloak.credential;

import io.cloudtrust.keycloak.AbstractRestApiExtensionTest;
import io.cloudtrust.keycloak.test.container.KeycloakDeploy;
import io.cloudtrust.keycloak.test.util.OidcTokenProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static io.cloudtrust.keycloak.credential.CtPasswordCreatedAdminEventMatcher.isPasswordCreatedAdminEvent;
import static io.cloudtrust.keycloak.credential.CtPasswordCreatedEventMatcher.isPasswordCreatedEvent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@ExtendWith(KeycloakDeploy.class)
class CtPasswordCredentialProviderTest extends AbstractRestApiExtensionTest {
    private static final Logger logger = Logger.getLogger(CtPasswordCredentialProviderTest.class);

    private AdminEventRepresentation pollIt() {
        AdminEventRepresentation res = adminEvents().poll();
        return res;
    }

    @Test
    void adminResetPassword() throws IOException, URISyntaxException {
        RealmResource testRealm = this.getRealm();
        UserRepresentation user = testRealm.users().search("test-user@localhost").get(0);

        Map<String, Object> body = Map.of(
                "type", "password",
                "value", "P@ssw0rd",
                "temporary", false);
        this.api().callJSON("PUT", "/admin/realms/test/users/" + user.getId() + "/reset-password", body);
        assertThat(pollIt(), isPasswordCreatedAdminEvent());
    }

    @Test
    void selfResetPassword() throws IOException, URISyntaxException {
        String clientId = "account";
        this.updateRealmClient("test", clientId, c -> c.setDirectAccessGrantsEnabled(true));

        OidcTokenProvider tokenProvider = this.createOidcTokenProvider(clientId, "");
        String currentPassword = "P@55w0rd!";
        String token = tokenProvider.getAccessToken("test-user@localhost", currentPassword);
        assertThat(token, is(not(nullValue())));
        this.events().clear();

        Map<String, Object> body = Map.of(
                "currentPassword", currentPassword,
                "newPassword", "P@ssw0rd",
                "confirmation", "P@ssw0rd");
        this.api().setToken(token);
        String resp = this.api().callJSON("POST", "/realms/master/api/account/realms/test/credentials/password", body);
        logger.infof("Update password response: %s", resp);

        assertThat(events().poll(), isPasswordCreatedEvent());
    }
}
