package io.cloudtrust.keycloak.restapi.email.model;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

public class UserWithOverridenEmail extends UserModelDelegate {
    private String email;

    public UserWithOverridenEmail(UserModel user, String email) {
        super(user);
        this.email = email;
    }

    @Override
    public String getEmail() {
        return StringUtils.defaultIfBlank(email, super.getEmail());
    }
}
