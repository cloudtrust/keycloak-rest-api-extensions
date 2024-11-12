package io.cloudtrust.keycloak.credential;

import io.cloudtrust.keycloak.AbstractRestApiExtensionTest;
import io.cloudtrust.keycloak.test.container.KeycloakDeploy;
import io.cloudtrust.keycloak.test.util.OidcTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static io.cloudtrust.keycloak.credential.CtPasswordCreatedAdminEventMatcher.isPasswordCreatedAdminEvent;
import static io.cloudtrust.keycloak.credential.CtPasswordCreatedEventMatcher.isPasswordCreatedEvent;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(KeycloakDeploy.class)
public class CtPasswordCredentialProviderTest extends AbstractRestApiExtensionTest {
    @Test
    void adminResetPassword() throws IOException, URISyntaxException {
        RealmResource testRealm = this.getRealm();
        UserRepresentation user = testRealm.users().search("test-user@localhost").get(0);

        Map<String, Object> body = new HashMap<>();
        body.put("type", "password");
        body.put("value", "P@ssw0rd");
        body.put("temporary", false);
        this.api().callJSON("PUT", "/admin/realms/test/users/" + user.getId() + "/reset-password", body);
        assertThat(adminEvents().poll(), isPasswordCreatedAdminEvent());
    }

    @Test
    void selfResetPassword() throws IOException, URISyntaxException {
        String clientId = "account";
        ClientRepresentation accountClient = this.getRealm("test")
                .clients()
                .findByClientId(clientId)
                .get(0);
        accountClient.setDirectAccessGrantsEnabled(true);
        this.getRealm("test")
                .clients()
                .get(accountClient.getId())
                .update(accountClient);

        OidcTokenProvider tokenProvider = this.createOidcTokenProvider(clientId, "");
        String token = tokenProvider.getAccessToken("test-user@localhost", "password");
        this.events().clear();

        Map<String, Object> body = new HashMap<>();
        body.put("currentPassword", "password");
        body.put("newPassword", "P@ssw0rd");
        body.put("confirmation", "P@ssw0rd");
        this.api().setToken(token);
        this.api().callJSON("POST", "/realms/master/api/account/realms/test/credentials/password", body);

        assertThat(events().poll(), isPasswordCreatedEvent());
    }
}
