package io.cloudtrust.keycloak.config;

import java.util.HashMap;
import java.util.Map;

public class KeycloakSystemVariables {

    private static final Map<String, String> OPTIONS = new HashMap<>();

    static {
        OPTIONS.put("spi-realm-restapi-extension-api-terms-of-use-acceptance-delay-days", "60");
    }

    public Map<String, String> load() {
        return new HashMap<>(OPTIONS);
    }
}
