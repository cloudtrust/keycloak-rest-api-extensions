package io.cloudtrust.keycloak.services.resource.api.account;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.common.Profile;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.account.AccountRestService;

import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This is a copy of org.keycloak.services.resources.account.AccountRestService that fixes a bug with CORS
 * The inspiration for the fix came from org.keycloak.services.resources.admin.AdminRoot.
 */
public class FixedAccountRestService extends AccountRestService {

    private KeycloakSession session;
    private Auth auth;
    private EventBuilder event;

    private UserModel user;
    private RealmModel realm;
    private EventBuilder adminEvent;

    @Context
    private HttpRequest request;

    public FixedAccountRestService(KeycloakSession session, Auth auth, ClientModel client, EventBuilder event) {
        super(session, auth, client, event);
        this.session = session;
        this.auth = auth;
        this.event = event;
        this.user = auth.getUser();
        this.realm = auth.getRealm();
    }

    @Path("/credentials")
    public FixedAccountCredentialResource credentials(@Context final HttpRequest request, @Context HttpResponse response) {
        Cors.add(request).allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().build(response);
        if (!Profile.isFeatureEnabled(Profile.Feature.ACCOUNT_API)) {
            throw new NotFoundException();
        }
        return new FixedAccountCredentialResource(session, event, user, auth);
    }

    /**
     * Delete account
     *
     * @return
     */
    @Path("/")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response deleteAccount() {
        auth.require(AccountRoles.MANAGE_ACCOUNT);

        UserModel user = auth.getUser();

        boolean removed = new UserManager(session).removeUser(realm, user);
        if (removed) {
            event.event(EventType.UPDATE_PROFILE).user(user)
                    .client(auth.getClient())
                    .detail("ct_event_type", "SELF_DELETE_ACCOUNT")
                    .detail("username", user.getUsername())
                    .success();
            return Cors.add(request, Response.ok()).auth().allowedOrigins(auth.getToken()).build();
        } else {
            event.event(EventType.UPDATE_PROFILE).user(user)
                    .client(auth.getClient())
                    .detail("ct_event_type", "SELF_DELETE_ACCOUNT_ERROR")
                    .detail("username", user.getUsername())
                    .success();
            return ErrorResponse.error("User couldn't be deleted", Response.Status.BAD_REQUEST);
        }
    }

}
