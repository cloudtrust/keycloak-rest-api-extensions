package io.cloudtrust.keycloak.restapi.services.resource.api.admin;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

public class CtRealmsAdminResource {
    private KeycloakSession session;
    private AdminAuth auth;

    public CtRealmsAdminResource(AdminAuth auth, KeycloakSession session) {
        this.auth = auth;
        this.session = session;
    }

    /**
     * Base path for the admin REST API for one particular realm.
     *
     * @param headers
     * @param name    realm name (not id!)
     * @return
     */
    @Path("{realm}")
    public CtRealmAdminResource getRealmAdmin(@PathParam("realm") @Parameter(description = "realm name (not id!)") final String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) throw new NotFoundException("Realm not found.");

        if (!auth.getRealm().equals(realmManager.getKeycloakAdministrationRealm()) && !auth.getRealm().equals(realm)) {
            throw new ForbiddenException();
        }
        // Synced with Keycloak 26.8.1 RealmsAdminResource#getRealmAdmin: the realm is set in the context before the permission evaluator is created
        session.getContext().setRealm(realm);
        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);

        ClientConnection clientConnection = session.getContext().getConnection();
        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, auth, session, clientConnection);

        return new CtRealmAdminResource(realmAuth, adminEvent, session);
    }
}
