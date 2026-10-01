package io.cloudtrust.keycloak.restapi.email.model;

import io.cloudtrust.keycloak.restapi.authentication.delegate.RealmModelDelegate;
import org.keycloak.models.RealmModel;

public class RealmWithOverridenEmailTheme extends RealmModelDelegate {
    private String emailTheme;

    public RealmWithOverridenEmailTheme(RealmModel realm) {
        super(realm);
        emailTheme = realm.getEmailTheme();
    }

    @Override
    public String getEmailTheme() {
        return emailTheme;
    }

    @Override
    public void setEmailTheme(String name) {
        emailTheme = name;
    }
}
