package io.cloudtrust.keycloak.services.api.idp;

import io.cloudtrust.keycloak.AbstractRestApiExtensionTest;
import io.cloudtrust.keycloak.config.ServerConfig;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = ServerConfig.class)
class CtIdpRootTest extends AbstractRestApiExtensionTest {

    @BeforeEach
    public void initToken() {
        this.api(keycloak, testRealm).initToken();
    }

    @Test
    void accessNonExistingRealmFails() throws IOException, URISyntaxException {
        String path = "/realms/master/api/idp/realms/non-existing-realm/team-members/some-user-id";
        try {
            api(keycloak, testRealm).call("DELETE", path);
            Assertions.fail("Expected 404 Not Found");
        } catch (HttpResponseException hre) {
            assertThat(hre.getStatusCode(), is(404));
        }
    }
}
