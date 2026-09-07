package io.cloudtrust.keycloak.services.resource.api.idp;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

public class CtIdpRealmsResource {
    private final AdminAuth auth;
    private final KeycloakSession session;

    public CtIdpRealmsResource(AdminAuth auth, KeycloakSession session) {
        this.auth = auth;
        this.session = session;
    }

    @Path("{realm}")
    public CtIdpRealmResource getRealmResource(@PathParam("realm") String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) {
            throw new NotFoundException("Realm not found.");
        }

        if (!auth.getRealm().equals(realmManager.getKeycloakAdministrationRealm()) && !auth.getRealm().equals(realm)) {
            throw new ForbiddenException();
        }

        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);
        session.getContext().setRealm(realm);

        ClientConnection clientConnection = session.getContext().getConnection();
        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, auth, session, clientConnection);
        return new CtIdpRealmResource(session, realmAuth, adminEvent);
    }
}
