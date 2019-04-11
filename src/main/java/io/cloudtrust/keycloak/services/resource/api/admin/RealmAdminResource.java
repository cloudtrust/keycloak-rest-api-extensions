package io.cloudtrust.keycloak.services.resource.api.admin;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.Path;

public class RealmAdminResource extends org.keycloak.services.resources.admin.RealmAdminResource {

    private final AdminEventBuilder adminEvent;

    public RealmAdminResource(AdminPermissionEvaluator auth, RealmModel realm, TokenManager tokenManager,
                              AdminEventBuilder adminEvent, KeycloakSession session) {
        super(auth, realm, tokenManager, adminEvent);
        this.adminEvent = adminEvent;
        this.session = session;
    }

    @Path("users")
    public UsersResource users() {
        UsersResource users = new UsersResource(session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(users);
        //resourceContext.initResource(users);
        return users;
    }

}
