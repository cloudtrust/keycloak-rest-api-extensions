package io.cloudtrust.keycloak.services.api.account;

import io.cloudtrust.keycloak.AbstractRestApiExtensionTest;
import io.cloudtrust.keycloak.test.container.KeycloakDeploy;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;

@ExtendWith(KeycloakDeploy.class)
public class AccountApiTest extends AbstractRestApiExtensionTest {
    private static final String USERNAME = "admin";

    @BeforeEach
    public void initToken() {
        this.api().initToken();
    }

    private void updateUser(String email, boolean isVerified) {
        RealmResource realm = getRealm();
        UserRepresentation user = this.getUserByName("master", USERNAME);
        user.setEmail(email);
        user.setEmailVerified(isVerified);
        UserResource userRes = realm.users().get(user.getId());
        userRes.update(user);
    }

    private boolean isVerified() throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            URI uri = new URIBuilder(this.getKeycloakURL() + "/realms/master/api/account/realms/master/email-verified").build();
            System.out.println(uri);

            HttpGet httpRequest = new HttpGet(uri);
            httpRequest.addHeader("Authorization", "Bearer " + this.api().getToken());
            HttpResponse response = client.execute(httpRequest);
            int code = response.getStatusLine().getStatusCode();
            if (code == HttpStatus.SC_NO_CONTENT) {
                return false;
            }
            if (code == HttpStatus.SC_ACCEPTED) {
                return true;
            }
            throw new IOException("Unexpected HTTP status code "+code);
        }
    }

    @ParameterizedTest
    @MethodSource("emailVerifiedSamples")
    @Disabled("Account API is currently not activated")
    void emailVerifiedTest(String email, boolean isVerified) throws IOException, URISyntaxException {
        updateUser(email, isVerified);
        Assertions.assertEquals(isVerified, isVerified());
    }

    public static Stream<Arguments> emailVerifiedSamples() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of("contact@trustid.ch", false),
                Arguments.of("contact@trustid.ch", true)
        );
    }
}
