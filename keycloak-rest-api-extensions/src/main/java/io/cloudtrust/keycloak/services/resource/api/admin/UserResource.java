package io.cloudtrust.keycloak.services.resource.api.admin;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class UserResource extends org.keycloak.services.resources.admin.UserResource {
    public UserResource(KeycloakSession session, UserModel user, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        super(session.getContext().getRealm(), user, auth, adminEvent);
    }
}
