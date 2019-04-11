package io.cloudtrust.keycloak.services.resource.api.account;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.common.Profile;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.account.AccountCredentialResource;
import org.keycloak.services.resources.account.AccountRestService;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

/**
 * This is a copy of org.keycloak.services.resources.account.AccountRestService that fixes a bug with CORS
 * The inspiration for the fix came from org.keycloak.services.resources.admin.AdminRoot.
 */
public class FixedAccountRestService extends AccountRestService {

    private KeycloakSession session;
    private Auth auth;
    private EventBuilder event;
    private UserModel user;

    public FixedAccountRestService(KeycloakSession session, Auth auth, ClientModel client, EventBuilder event) {
        super(session, auth, client, event);
        this.session = session;
        this.auth = auth;
        this.event = event;
        this.user = auth.getUser();
    }

    @Path("/credentials")
    public AccountCredentialResource credentials(@Context final HttpRequest request, @Context HttpResponse response) {
        Cors.add(request).allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().build(response);
        if (!Profile.isFeatureEnabled(Profile.Feature.ACCOUNT_API)) {
            throw new NotFoundException();
        }
        return new AccountCredentialResource(session, event, user, auth);
    }
}
