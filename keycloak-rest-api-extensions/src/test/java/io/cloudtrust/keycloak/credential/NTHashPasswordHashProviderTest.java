package io.cloudtrust.keycloak.credential;

import org.junit.jupiter.api.Test;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.models.credential.dto.PasswordSecretData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class NTHashPasswordHashProviderTest {

    @Test
    void encodeVerifyWorks() {
        NTHashPasswordHashProvider hashProvider = new NTHashPasswordHashProvider();
        PasswordCredentialModel encodedCredential = hashProvider.encodedCredential("password", 1);

        assertThat(hashProvider.verify("password", encodedCredential), is(true));
        assertThat(hashProvider.verify("wrong-password", encodedCredential), is(false));
    }

    @Test
    void verifyWorks() {
        String rawPassword = "P@ssw0rd";

        NTHashPasswordHashProvider hashProvider = new NTHashPasswordHashProvider();
        PasswordCredentialData credentialData = new PasswordCredentialData(1, "nthash");
        byte[] salt = {};
        PasswordSecretData secretData = new PasswordSecretData("E19CCF75EE54E06B06A5907AF13CEF42", salt);
        PasswordCredentialModel result = PasswordCredentialModel.createFromValues(credentialData, secretData);

        assertThat(hashProvider.verify(rawPassword, result), is(true));
    }
}
