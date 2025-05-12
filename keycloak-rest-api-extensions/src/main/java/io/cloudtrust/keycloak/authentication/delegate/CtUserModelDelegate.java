package io.cloudtrust.keycloak.authentication.delegate;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

public class CtUserModelDelegate extends UserModelDelegate {
    private String email;

    public CtUserModelDelegate(UserModel user) {
        super(user);
        email = user.getEmail();
    }

    @Override
    public String getEmail() {
        return this.email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }
}
