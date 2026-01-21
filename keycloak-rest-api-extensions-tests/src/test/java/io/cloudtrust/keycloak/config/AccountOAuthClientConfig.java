package io.cloudtrust.keycloak.config;

import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.realm.ClientConfigBuilder;

public class AccountOAuthClientConfig extends DefaultOAuthClientConfiguration {
    public static final String CLIENT_ID = "account";

    @Override
    public ClientConfigBuilder configure(ClientConfigBuilder client) {
        return client.clientId(CLIENT_ID);
    }
}
