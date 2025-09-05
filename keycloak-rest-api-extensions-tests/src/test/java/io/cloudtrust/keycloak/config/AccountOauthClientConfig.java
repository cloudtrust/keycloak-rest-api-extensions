package io.cloudtrust.keycloak.config;

import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.realm.ClientConfigBuilder;

public class AccountOauthClientConfig extends DefaultOAuthClientConfiguration {

    @Override
    public ClientConfigBuilder configure(ClientConfigBuilder client) {
        return client
                .clientId("account");
    }

}
