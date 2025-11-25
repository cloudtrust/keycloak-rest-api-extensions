package io.cloudtrust.keycloak.config;

import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class ServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder keycloakServerConfigBuilder) {
        return keycloakServerConfigBuilder
                .option("spi-realm-restapi-extension-api-terms-of-use-acceptance-delay-days", "60")
                .dependency("io.cloudtrust", "cloudtrust-common")
                .dependency("io.cloudtrust", "kc-cloudtrust-common")
                .dependency("io.cloudtrust", "keycloak-rest-api-extensions");
    }

}
