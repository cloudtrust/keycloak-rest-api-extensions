package io.cloudtrust.keycloak.config;

import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class ServerConfig implements KeycloakServerConfig {

    private final KeycloakSystemVariables keycloakSystemVariables;

    public ServerConfig() {
        this.keycloakSystemVariables = new KeycloakSystemVariables();
    }

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder keycloakServerConfigBuilder) {
        return keycloakServerConfigBuilder
                .options(keycloakSystemVariables.load())
                .dependency("io.cloudtrust", "cloudtrust-common")
                .dependency("io.cloudtrust", "kc-cloudtrust-common")
                .dependency("io.cloudtrust", "keycloak-rest-api-extensions");
    }

}
