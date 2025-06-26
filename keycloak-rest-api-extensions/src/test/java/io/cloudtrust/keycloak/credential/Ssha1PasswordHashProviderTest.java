package io.cloudtrust.keycloak.credential;

import org.junit.jupiter.api.Test;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.models.credential.dto.PasswordSecretData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class Ssha1PasswordHashProviderTest {

    @Test
    void encodeVerifyWorks() {
        Ssha1PasswordHashProvider hashProvider = new Ssha1PasswordHashProvider();
        PasswordCredentialModel encodedCredential = hashProvider.encodedCredential("password", 1);

        assertThat(hashProvider.verify("password", encodedCredential), is(true));
        assertThat(hashProvider.verify("wrong-password", encodedCredential), is(false));
    }

    @Test
    void verifyWorks() {
        String rawPassword = "Blub1234!Blub";

        Ssha1PasswordHashProvider hashProvider = new Ssha1PasswordHashProvider();
        PasswordCredentialData credentialData = new PasswordCredentialData(1, "SSHA1");
        byte[] salt = {};
        PasswordSecretData secretData = new PasswordSecretData("{SSHA}qlZRsgVNc96hopKzDPK15gt12n2ITkcuB8PDp1VR", salt);
        PasswordCredentialModel result = PasswordCredentialModel.createFromValues(credentialData, secretData);

        assertThat(hashProvider.verify(rawPassword, result), is(true));
    }
}
