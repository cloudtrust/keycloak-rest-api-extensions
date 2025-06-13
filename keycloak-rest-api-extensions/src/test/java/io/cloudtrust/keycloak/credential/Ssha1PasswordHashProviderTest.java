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
        String rawPassword = "H31103_Ca!";

        Ssha1PasswordHashProvider hashProvider = new Ssha1PasswordHashProvider();
        PasswordCredentialData credentialData = new PasswordCredentialData(1, "SSHA1");
        byte[] salt = {};
        PasswordSecretData secretData = new PasswordSecretData("{SSHA1}Pq6+uU0cefbLUmw9xPIcAKMroznfgWEZNlJRFlB87qkzlxAPJ47cZFBHFVpTtHG0wCAwCg==", salt);
        PasswordCredentialModel result = PasswordCredentialModel.createFromValues(credentialData, secretData);

        PasswordSecretData secretData2 = new PasswordSecretData("{SSHA1}l5TiEgWwfFDg1iVb6AyQ7KAL4r2d0u9y73z027DOVwowaBWAcjJ3DvNseR2HvFCmNyu5qQ==", salt);
        PasswordCredentialModel result2 = PasswordCredentialModel.createFromValues(credentialData, secretData2);

        assertThat(hashProvider.verify(rawPassword, result), is(true));
        assertThat(hashProvider.verify(rawPassword, result2), is(true));
    }
}
