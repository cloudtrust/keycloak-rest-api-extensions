package io.cloudtrust.keycloak.restapi.services.api.admin;

import io.cloudtrust.keycloak.restapi.AbstractRestApiExtensionTest;
import io.cloudtrust.keycloak.ExecuteActionsEmailHelper;
import io.cloudtrust.keycloak.restapi.config.ServerConfig;
import io.cloudtrust.keycloak.restapi.email.model.EmailModel;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest(config = ServerConfig.class)
class CtUserResourceTest extends AbstractRestApiExtensionTest {
    @InjectMailServer
    MailServer mailServer;

    @InjectUser(config = DisabledUser.class, realmRef = "test")
    ManagedUser disabledUser;

    private static final String[] ACTIONS = new String[]{ExecuteActionsEmailHelper.VERIFY_EMAIL_ACTION};
    private static final String SEND_EMAIL_FMT = "/realms/master/api/admin/realms/test/users/%s/send-email";
    private static final String EXECUTE_ACTIONS_EMAIL_FMT = "/realms/master/api/admin/realms/test/users/%s/execute-actions-email";

    @Test
    void sendEmailToDisabledUser() throws MessagingException, IOException, URISyntaxException {
        sendEmailFails(disabledUser, new EmailModel());
    }

    private void sendEmailFails(ManagedUser user, EmailModel emailModel) throws IOException, URISyntaxException, MessagingException {
        var path = String.format(SEND_EMAIL_FMT, user.getId());
        var thrown = Assertions.assertThrows(HttpResponseException.class, () ->
            api(keycloak, testRealm).callJSON("POST", path, emailModel)
        );
        assertThat(thrown.getMessage(), containsString("400"));
    }

    @Test
    void sendExecuteActionsEmailNominalCaseTest() throws IOException, URISyntaxException, MessagingException {
        sendExecuteActionsEmailSuccessCase("");
    }

    @Test
    void sendExecuteActionsEmailUseOtherRealmThemeSuccessTest() throws IOException, URISyntaxException, MessagingException {
        // dummy2 is configured with a valid theme
        sendExecuteActionsEmailSuccessCase("?themeRealm=dummy2");
    }

    @Test
    void sendExecuteActionsEmailUseOtherRealmThemeFailureTest() throws IOException, URISyntaxException, MessagingException {
        // dummy1 is configured with a non-existing theme. It was making the processing fail with an internal server error
        // Between 8.0.1 and 13.0.0, Keycloak loads a default theme if provided theme is an invalid one
        sendExecuteActionsEmailSuccessCase("?themeRealm=dummy1");
    }

    @Test
    void sendExecuteActionsEmailRealmNotFoundTest() throws IOException, URISyntaxException {
        // unknown realm does not exist
        sendExecuteActionsEmailFailureCase("?themeRealm=unknown", 400);
    }

    private void sendExecuteActionsEmailSuccessCase(String queryParams) throws IOException, URISyntaxException, MessagingException {
        var id = testRealm.admin().users().search("john-doh@localhost").getFirst().getId();
        var path = String.format(EXECUTE_ACTIONS_EMAIL_FMT, id);
        var nbReceived = mailServer.getReceivedMessages().length;

        api(keycloak, testRealm).callJSON("PUT", path + queryParams, ACTIONS);
        assertThat(mailServer.getReceivedMessages().length, is(nbReceived + 1));
        var mail = mailServer.getReceivedMessages()[nbReceived];
        assertThat(mail, is(not(nullValue())));
        String mailContent = getMailContent(mail);
        assertThat(mailContent, containsString("ct-verify-email"));
    }

    private void sendExecuteActionsEmailFailureCase(String queryParams, int expectedHttpStatusCode) throws IOException, URISyntaxException {
        var id = testRealm.admin().users().search("john-doh@localhost").getFirst().getId();
        var path = String.format(EXECUTE_ACTIONS_EMAIL_FMT, id);
        var nbReceived = mailServer.getReceivedMessages().length;

        try {
            this.api(keycloak, testRealm).callJSON("PUT", path + queryParams, ACTIONS);
            Assertions.fail();
        } catch (HttpResponseException hre) {
            assertThat(hre.getStatusCode(), is(expectedHttpStatusCode));
            assertThat(mailServer.getReceivedMessages().length, is(nbReceived));
        }
    }

    private String getMailContent(MimeMessage mail) throws IOException, MessagingException {
        return new String(mail.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    static class DisabledUser implements UserConfig {
        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.username("disabled-one").email("disabled-one@localhost").enabled(false);
        }
    }
}
