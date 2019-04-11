package io.cloudtrust.keycloak.services.resource.api.admin;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.Path;

public class UserResource extends org.keycloak.services.resources.admin.UserResource {

    private AdminPermissionEvaluator auth;

    private AdminEventBuilder adminEvent;
    private UserModel user;

    public UserResource(KeycloakSession session, UserModel user, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        super(session.getContext().getRealm(), user, auth, adminEvent);
        this.auth = auth;
        this.user = user;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
    }

    @Path("credentials")
    public CredentialsResource credentials() {
        CredentialsResource resource = new CredentialsResource(session, user, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

}
