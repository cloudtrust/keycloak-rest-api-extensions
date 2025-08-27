package io.cloudtrust.keycloak.config.realm;

import io.cloudtrust.keycloak.config.AbstractRealmConfig;

public class TestRealm extends AbstractRealmConfig {
    public TestRealm() {
        super("/testrealm.json");
    }
}
