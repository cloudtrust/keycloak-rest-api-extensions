package io.cloudtrust.keycloak.services.api.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import io.cloudtrust.keycloak.AbstractRestApiExtensionTest;
import io.cloudtrust.keycloak.representations.idm.DeletableUserRepresentation;
import io.cloudtrust.keycloak.services.resource.api.model.EmailInfo;
import io.cloudtrust.keycloak.test.container.KeycloakDeploy;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(KeycloakDeploy.class)
class CtAdminRootTest extends AbstractRestApiExtensionTest {
    private static final TypeReference<List<DeletableUserRepresentation>> deletableUserListType = new TypeReference<List<DeletableUserRepresentation>>() {
    };
    private static final TypeReference<List<EmailInfo>> emailInfoListType = new TypeReference<>() {
    };

    @BeforeEach
    public void initToken() {
        this.api().initToken();
    }

    @Test
    void testNoUserDeclinedTOU() throws IOException, URISyntaxException {
        // To write a test where there are some users having declined TOU for more than the configured delay,
        // we should create sample users with a given creation timestamp far in the past
        List<NameValuePair> params = Collections.emptyList();
        List<DeletableUserRepresentation> users = api().query(deletableUserListType, "GET", "/realms/master/api/admin/expired-tou-acceptance", params);
        assertThat(users.size(), is(0));
    }

    @Test
    void testGetSupportInformation() throws IOException, URISyntaxException {
        List<NameValuePair> params = Collections.singletonList(new BasicNameValuePair("email", "john-doh@LOCALHOST"));
        List<EmailInfo> emailInfo = api().query(emailInfoListType, "GET", "/realms/master/api/admin/support-infos", params);
        assertThat(emailInfo.size(), is(1));
    }
}
