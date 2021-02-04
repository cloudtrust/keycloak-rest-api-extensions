package io.cloudtrust.keycloak.services.api.admin;

import io.cloudtrust.keycloak.test.ApiTest;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpResponseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.util.GreenMailRule;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class CtUserResourceTest extends ApiTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule();

    private static final String[] ACTIONS = new String[]{"VERIFY_EMAIL"};
    private static final String EXECUTE_ACTIONS_EMAIL_FMT = "/realms/master/api/admin/realms/test/users/%s/execute-actions-email";

    @Before
    public void createDummyRealms() {
        createDummyRealm("dummy1", "invalid-theme");
        createDummyRealm("dummy2", "keycloak");
    }

    private void createDummyRealm(String realm, String theme) {
        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setId(realm);
        realmRepresentation.setRealm(realm);
        realmRepresentation.setEnabled(true);
        realmRepresentation.setEmailTheme(theme);
        keycloak.realms().create(realmRepresentation);
    }

    @After
    public void deleteDummyRealms() {
        keycloak.realm("dummy1").remove();
        keycloak.realm("dummy2").remove();
    }

    @Test
    public void sendExecuteActionsEmailNominalCaseTest() throws IOException, URISyntaxException, MessagingException {
        successCase("");
    }

    @Test
    public void sendExecuteActionsEmailUseOtherRealmThemeSuccessTest() throws IOException, URISyntaxException, MessagingException {
        // dummy2 is configured with a valid theme
        successCase("?themeRealm=dummy2");
    }

    @Test
    public void sendExecuteActionsEmailUseOtherRealmThemeFailureTest() throws IOException, URISyntaxException {
        // dummy1 is configured with a non-existing theme. It makes the processing fail with an internal server error
        failureCase("?themeRealm=dummy1", 500);
    }

    @Test
    public void sendExecuteActionsEmailRealmNotFoundTest() throws IOException, URISyntaxException {
        // unknown realm does not exist
        failureCase("?themeRealm=unknown", 400);
    }

    private void successCase(String queryParams) throws IOException, URISyntaxException, MessagingException {
        String id = testRealm.users().search("john-doh@localhost").get(0).getId();
        String path = String.format(EXECUTE_ACTIONS_EMAIL_FMT, id);
        int nbReceived = greenMail.getReceivedMessages().length;

        callApiJSON("PUT", path + queryParams, ACTIONS);
        assertThat(greenMail.getReceivedMessages().length, is(nbReceived + 1));
        MimeMessage mail = greenMail.getLastReceivedMessage();
        assertThat(mail, is(not(nullValue())));
        String mailContent = getMailContent(mail);
        assertThat(mailContent, containsString("Verify Email"));
    }

    private void failureCase(String queryParams, int expectedHttpStatusCode) throws IOException, URISyntaxException {
        String id = testRealm.users().search("john-doh@localhost").get(0).getId();
        String path = String.format(EXECUTE_ACTIONS_EMAIL_FMT, id);
        int nbReceived = greenMail.getReceivedMessages().length;

        try {
            callApiJSON("PUT", path + queryParams, ACTIONS);
            Assert.fail();
        } catch (HttpResponseException hre) {
            assertThat(hre.getStatusCode(), is(expectedHttpStatusCode));
            assertThat(greenMail.getReceivedMessages().length, is(nbReceived));
        }
    }

    private String getMailContent(MimeMessage mail) throws IOException, MessagingException {
        StringWriter writer = new StringWriter();
        String encoding = StandardCharsets.UTF_8.name();
        IOUtils.copy(mail.getInputStream(), writer, encoding);
        return writer.toString();
    }
}
