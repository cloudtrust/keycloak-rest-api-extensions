package io.cloudtrust.keycloak.services.api.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import io.cloudtrust.keycloak.representations.idm.DeletableUserRepresentation;
import io.cloudtrust.keycloak.services.resource.api.model.EmailInfo;
import io.cloudtrust.keycloak.test.ApiTest;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AdminRootTest extends ApiTest {
    private static final TypeReference<List<DeletableUserRepresentation>> deletableUserListType = new TypeReference<List<DeletableUserRepresentation>>() {
    };
    private static final TypeReference<List<EmailInfo>> emailInfoListType = new TypeReference<List<EmailInfo>>() {
    };

    protected <T> T queryApi(TypeReference<T> typeRef, String method, String apiPath, List<NameValuePair> params) throws IOException, URISyntaxException {
        return mapper.readValue(callApi(method, apiPath, params), typeRef);
    }

    @Test
    public void testNoUserDeclinedTOU() throws IOException, URISyntaxException {
        // To write a test where there are some users having declined TOU for more than the configured delay,
        // we should create sample users with a given creation timestamp far in the past
        List<NameValuePair> params = Collections.emptyList();
        List<DeletableUserRepresentation> users = queryApi(deletableUserListType, "GET", "/realms/master/api/admin/expired-tou-acceptance", params);
        assertThat(users.size(), is(0));
    }

    @Test
    public void testGetSupportInformation() throws IOException, URISyntaxException {
        List<NameValuePair> params = Collections.singletonList(new BasicNameValuePair("email", "john-doh@localhost"));
        List<EmailInfo> emailInfo = queryApi(emailInfoListType, "GET", "/realms/master/api/admin/support-infos", params);
        assertThat(emailInfo.size(), is(1));
    }
}
