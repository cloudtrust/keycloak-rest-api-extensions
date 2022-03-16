package io.cloudtrust.keycloak.services.resource.api.admin;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

public class RealmsAdminResource extends org.keycloak.services.resources.admin.RealmsAdminResource {
    public RealmsAdminResource(AdminAuth auth, TokenManager tokenManager, KeycloakSession session) {
        super(auth, tokenManager);
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
    }

    /**
     * Base path for the admin REST API for one particular realm.
     *
     * @param headers
     * @param name    realm name (not id!)
     * @return
     */
    @Path("{realm}")
    @Override
    public org.keycloak.services.resources.admin.RealmAdminResource getRealmAdmin(@Context final HttpHeaders headers, @PathParam("realm") final String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) throw new NotFoundException("Realm not found.");

        if (!auth.getRealm().equals(realmManager.getKeycloakAdminstrationRealm()) && !auth.getRealm().equals(realm)) {
            throw new ForbiddenException();
        }
        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);

        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, auth, session, clientConnection);
        session.getContext().setRealm(realm);

        org.keycloak.services.resources.admin.RealmAdminResource adminResource = new RealmAdminResource(realmAuth, realm, tokenManager, adminEvent, session);
        ResteasyProviderFactory.getInstance().injectProperties(adminResource);
        return adminResource;
    }
}
