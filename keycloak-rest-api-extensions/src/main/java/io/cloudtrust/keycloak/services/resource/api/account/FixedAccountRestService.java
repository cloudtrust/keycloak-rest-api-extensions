package io.cloudtrust.keycloak.services.resource.api.account;

import io.cloudtrust.keycloak.email.model.UserWithOverridenEmail;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.common.Profile;
import org.keycloak.common.enums.AccountRestApiVersion;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.account.AccountRestService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @Context
    private HttpRequest request;

    public FixedAccountRestService(KeycloakSession session, Auth auth, ClientModel client, EventBuilder event) {
        super(session, auth, client, event, AccountRestApiVersion.V1_ALPHA1);
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
     * @param rep
     * @return
     */
    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response updateAccount(UserRepresentation rep) {
        boolean emailUpdated = user != null && rep.getEmail() != null && !rep.getEmail().equalsIgnoreCase(user.getEmail());
        Response resp = super.updateAccount(rep);
        if (emailUpdated && resp.getStatus() < 400) {
            /**
             * EmailVerified is not updatable through KC API in version 14.0
             */
            user.setEmailVerified(false);
        }
        return resp;
    }

    /**
     * Delete account
     *
     * @return REST response
     */
    @Path("/")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response deleteAccount() {
        auth.require(AccountRoles.MANAGE_ACCOUNT);

        UserModel delUser = auth.getUser();

        boolean removed = new UserManager(session).removeUser(realm, delUser);
        if (removed) {
            event.event(EventType.UPDATE_PROFILE).user(delUser)
                    .client(auth.getClient())
                    .detail("ct_event_type", "SELF_DELETE_ACCOUNT")
                    .detail("username", delUser.getUsername())
                    .success();
            return Cors.add(request, Response.noContent()).auth().allowedOrigins(auth.getToken()).build();
        } else {
            event.event(EventType.UPDATE_PROFILE).user(delUser)
                    .client(auth.getClient())
                    .detail("ct_event_type", "SELF_DELETE_ACCOUNT_ERROR")
                    .detail("username", delUser.getUsername())
                    .success();
            return ErrorResponse.error("User couldn't be deleted", Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Send execute actions email
     */
    @Path("execute-actions-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response executeActionsEmail(@QueryParam("lifespan") Integer lifespan, List<String> actions) {
        if (user.getEmail() == null) {
            return ErrorResponse.error("User email missing", Status.BAD_REQUEST);
        }

        if (!user.isEnabled()) {
            throw new WebApplicationException(
                    ErrorResponse.error("User is disabled", Status.BAD_REQUEST));
        }

        if (lifespan == null) {
            lifespan = realm.getActionTokenGeneratedByAdminLifespan();
        }
        int expiration = Time.currentTime() + lifespan;
        String redirectUri = null;
        String clientId = Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
        ExecuteActionsActionToken token = new ExecuteActionsActionToken(user.getId(), expiration, actions, redirectUri, clientId);

        try {
            UriBuilder builder = LoginActionsService.actionTokenProcessor(session.getContext().getUri());
            builder.queryParam("key", token.serialize(session, realm, session.getContext().getUri()));

            String link = builder.build(realm.getName()).toString();

            this.session.getProvider(EmailTemplateProvider.class)
                    .setAttribute(Constants.TEMPLATE_ATTR_REQUIRED_ACTIONS, token.getRequiredActions())
                    .setRealm(realm)
                    .setUser(user)
                    .sendExecuteActions(link, TimeUnit.SECONDS.toMinutes(lifespan));

            return Response.noContent().build();
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendActionsEmail(e);
            return ErrorResponse.error("Failed to send execute actions email", Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Path("send-email")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendMail(@QueryParam("subject") String subjectFormatKey, @QueryParam("template") String template, @QueryParam("recipient") String recipient, Map<String, String> parameters) {
        if (StringUtils.isBlank(template)) {
            return ErrorResponse.error("Template email missing", Status.BAD_REQUEST);
        }
        if (StringUtils.isBlank(subjectFormatKey)) {
            return ErrorResponse.error("Subject missing", Status.BAD_REQUEST);
        }
        UserWithOverridenEmail userWithEmail = new UserWithOverridenEmail(user, recipient);
        if (StringUtils.isBlank(userWithEmail.getEmail())) {
            return ErrorResponse.error("User email missing", Status.BAD_REQUEST);
        }

        if (!user.isEnabled()) {
            throw new WebApplicationException(
                    ErrorResponse.error("User is disabled", Status.BAD_REQUEST));
        }

        UriBuilder builder = LoginActionsService.loginActionsBaseUrl(session.getContext().getUri()).path(getClass(), "sendMail");
        String link = builder.build(realm.getName()).toString();

        KeycloakContext context = session.getContext();
        EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class)
                .setRealm(context.getRealm())
                .setUser(userWithEmail)
                .setAuthenticationSession(context.getAuthenticationSession());
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", new ProfileBean(user));
        attributes.put("realmName", realm.getDisplayName());
        attributes.put("link", link);
        parameters.forEach(attributes::put);

        try {
            emailProvider.send(subjectFormatKey, template, attributes);
            return Response.noContent().build();
        } catch (EmailException e) {
            throw new WebApplicationException("Can't send mail to " + userWithEmail.getEmail(), e);
        }
    }
}
