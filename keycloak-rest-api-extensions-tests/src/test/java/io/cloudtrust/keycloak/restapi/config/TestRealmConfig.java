package io.cloudtrust.keycloak.restapi.config;

public class TestRealmConfig extends AbstractRealmConfig {
    public TestRealmConfig() {
        super("/testrealm.json");
    }
}
