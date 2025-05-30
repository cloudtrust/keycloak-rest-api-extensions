package io.cloudtrust.keycloak.services.api.admin;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.cloudtrust.keycloak.AbstractRestApiExtensionTest;
import io.cloudtrust.keycloak.ExecuteActionsEmailHelper;
import io.cloudtrust.keycloak.test.container.KeycloakDeploy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@ExtendWith(KeycloakDeploy.class)
class CtUserResourceTest extends AbstractRestApiExtensionTest {
    private static GreenMail greenMail;

    private static final String[] ACTIONS = new String[]{ExecuteActionsEmailHelper.VERIFY_EMAIL_ACTION};
    private static final String EXECUTE_ACTIONS_EMAIL_FMT = "/realms/master/api/admin/realms/test/users/%s/execute-actions-email";

    @BeforeAll
    public static void setupGreenMail() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
    }

    @AfterAll
    public static void shutdownGreenMail() {
        greenMail.stop();
    }

    @Test
    void sendExecuteActionsEmailNominalCaseTest() throws IOException, URISyntaxException, MessagingException {
        successCase("");
    }

    @Test
    void sendExecuteActionsEmailUseOtherRealmThemeSuccessTest() throws IOException, URISyntaxException, MessagingException {
        // dummy2 is configured with a valid theme
        successCase("?themeRealm=dummy2");
    }

    @Test
    void sendExecuteActionsEmailUseOtherRealmThemeFailureTest() throws IOException, URISyntaxException, MessagingException {
        // dummy1 is configured with a non-existing theme. It was making the processing fail with an internal server error
        // Between 8.0.1 and 13.0.0, Keycloak loads a default theme if provided theme is an invalid one
        successCase("?themeRealm=dummy1");
    }

    @Test
    void sendExecuteActionsEmailRealmNotFoundTest() throws IOException, URISyntaxException {
        // unknown realm does not exist
        failureCase("?themeRealm=unknown", 400);
    }

    private void successCase(String queryParams) throws IOException, URISyntaxException, MessagingException {
        String id = this.getRealm().users().search("john-doh@localhost").get(0).getId();
        String path = String.format(EXECUTE_ACTIONS_EMAIL_FMT, id);
        int nbReceived = greenMail.getReceivedMessages().length;

        this.api().callJSON("PUT", path + queryParams, ACTIONS);
        assertThat(greenMail.getReceivedMessages().length, is(nbReceived + 1));
        MimeMessage mail = greenMail.getReceivedMessages()[nbReceived];
        assertThat(mail, is(not(nullValue())));
        String mailContent = getMailContent(mail);
        assertThat(mailContent, containsString("ct-verify-email"));
    }

    private void failureCase(String queryParams, int expectedHttpStatusCode) throws IOException, URISyntaxException {
        String id = this.getRealm().users().search("john-doh@localhost").get(0).getId();
        String path = String.format(EXECUTE_ACTIONS_EMAIL_FMT, id);
        int nbReceived = greenMail.getReceivedMessages().length;

        try {
            this.api().callJSON("PUT", path + queryParams, ACTIONS);
            Assertions.fail();
        } catch (HttpResponseException hre) {
            assertThat(hre.getStatusCode(), is(expectedHttpStatusCode));
            assertThat(greenMail.getReceivedMessages().length, is(nbReceived));
        }
    }

    private String getMailContent(MimeMessage mail) throws IOException, MessagingException {
        return new String(mail.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
