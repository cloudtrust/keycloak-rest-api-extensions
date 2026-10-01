package io.cloudtrust.keycloak.restapi.services.resource.api;

public class ApiConfig {
    private long termsOfUseAcceptanceDelay;

    public long getTermsOfUseAcceptanceDelayMillis() {
        return termsOfUseAcceptanceDelay;
    }

    public void setTermsOfUseAcceptanceDelayMillis(long delay) {
        this.termsOfUseAcceptanceDelay = delay;
    }
}
