package io.cloudtrust.keycloak.credential;

import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.credential.PasswordCredentialProviderFactory;
import org.keycloak.models.KeycloakSession;

public class CtPasswordCredentialProviderFactory extends PasswordCredentialProviderFactory {

    @Override
    public PasswordCredentialProvider create(KeycloakSession session) {
        return new CtPasswordCredentialProvider(session);
    }
}
