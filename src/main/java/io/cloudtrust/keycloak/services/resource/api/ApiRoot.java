package io.cloudtrust.keycloak.services.resource.api;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

public class ApiRoot {

    protected static final Logger logger = Logger.getLogger(ApiRoot.class);

    private final KeycloakSession session;
    private final RealmModel realm;

    public ApiRoot(KeycloakSession session) {
        this.session = session;
        realm = session.getContext().getRealm();
    }

    /**
     * Base path for managing users in this realm.
     *
     * @return
     */
    @Path("users")
    public UsersResource users() {

        AdminAuth auth = authenticateRealmAdminRequest();
        if (auth != null) {
            logger.debug("authenticated admin access for: " + auth.getUser().getUsername());
        }
        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);
        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, auth, session, session.getContext().getConnection());

        UsersResource users = new UsersResource(session, realmAuth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(users);
        return users;
    }

    private AdminAuth authenticateRealmAdminRequest() {
        AuthenticationManager.AuthResult auth = new AppAuthManager().authenticateBearerToken(session, session.getContext().getRealm());
        if (auth == null) {
            logger.debug("Token not valid");
            throw new UnauthorizedException("Bearer");
        }
        ClientModel client = realm.getClientByClientId(auth.getToken().getIssuedFor());
        if (client == null) {
            throw new NotFoundException("Could not find client for authorization");

        }
        return new AdminAuth(realm, auth.getToken(), auth.getUser(), client);
    }
}
