package io.cloudtrust.keycloak.services.api.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import io.cloudtrust.keycloak.AbstractRestApiExtensionTest;
import io.cloudtrust.keycloak.config.ServerConfig;
import io.cloudtrust.keycloak.representations.idm.DeletableUserRepresentation;
import io.cloudtrust.keycloak.services.resource.api.model.EmailInfo;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = ServerConfig.class)
class CtAdminRootTest extends AbstractRestApiExtensionTest {

    private static final TypeReference<List<DeletableUserRepresentation>> deletableUserListType = new TypeReference<>() {};

    private static final TypeReference<List<EmailInfo>> emailInfoListType = new TypeReference<>() {};


    @BeforeEach
    public void initToken() {
        api().initToken();
    }

    @Test
    void testNoUserDeclinedTOU() throws IOException, URISyntaxException {
        // To write a test where there are some users having declined TOU for more than the configured delay,
        // we should create sample users with a given creation timestamp far in the past
        List<NameValuePair> params = Collections.emptyList();
        List<DeletableUserRepresentation> users = api()
                .query(deletableUserListType, "GET", "/realms/master/api/admin/expired-tou-acceptance", params);
        assertThat(users.size(), is(0));
    }

    @Test
    void testGetSupportInformation() throws IOException, URISyntaxException {
        String username = "testuser";
        String email = "search@me.com";
        long testStart = System.currentTimeMillis();

        createUser(testRealm, username, u-> u.setEmail(email));

        List<NameValuePair> params = Collections.singletonList(new BasicNameValuePair("email", email));
        List<EmailInfo> emailInfo = api().query(emailInfoListType, "GET", "/realms/master/api/admin/support-infos", params);
        assertThat(emailInfo.size(), is(1));
        EmailInfo found = emailInfo.getFirst();
        assertThat(found.getRealm(), is("test"));
        assertThat(testStart<found.getCreationDate(), is(true));
        assertThat(found.getCreationDate()-testStart< Duration.ofSeconds(10).toMillis(), is(true));
    }
}
