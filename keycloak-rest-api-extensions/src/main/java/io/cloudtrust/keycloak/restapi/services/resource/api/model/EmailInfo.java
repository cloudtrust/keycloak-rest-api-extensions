package io.cloudtrust.keycloak.restapi.services.resource.api.model;

public class EmailInfo {
    private String realm;
    private Long creationDate;

    public String getRealm() {
        return this.realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Long getCreationDate() {
        return this.creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }
}
