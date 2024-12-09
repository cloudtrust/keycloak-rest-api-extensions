package io.cloudtrust.keycloak.credential;

import org.junit.jupiter.api.Test;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.models.credential.dto.PasswordSecretData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class Ssha256PasswordHashProviderTest {

    @Test
    void encodeVerifyWorks() {
        Ssha256PasswordHashProvider hashProvider = new Ssha256PasswordHashProvider();
        PasswordCredentialModel encodedCredential = hashProvider.encodedCredential("password", 1);

        assertThat(hashProvider.verify("password", encodedCredential), is(true));
        assertThat(hashProvider.verify("wrong-password", encodedCredential), is(false));
    }

    @Test
    void verifyWorks() {
        Ssha256PasswordHashProvider hashProvider = new Ssha256PasswordHashProvider();
        PasswordCredentialData credentialData = new PasswordCredentialData(1, "SSHA256");
        byte[] salt = {};
        PasswordSecretData secretData = new PasswordSecretData("{SSHA256}M2612WkI8a6ekLkqzzoihJB/YvW5wrt3Hgdr5NTubAMzOTd6cWNKcGxBOEVUNzNNUXZMcS9HU0hoVkhxZUd1dg==", salt);
        PasswordCredentialModel result = PasswordCredentialModel.createFromValues(credentialData, secretData);

        assertThat(hashProvider.verify("H31103_Ca!", result), is(true));
    }
}
