package io.cloudtrust.keycloak.services.resource.api.account;

import io.cloudtrust.keycloak.ExecuteActionsEmailHelper;
import io.cloudtrust.keycloak.authentication.delegate.CtUserModelDelegate;
import io.cloudtrust.keycloak.email.model.UserWithOverridenEmail;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.common.Profile;
import org.keycloak.common.enums.AccountRestApiVersion;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.account.AccountRestService;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a copy of org.keycloak.services.resources.account.AccountRestService that fixes a bug with CORS
 * The inspiration for the fix came from org.keycloak.services.resources.admin.AdminRoot.
 */
public class FixedAccountRestService {
    private static final Logger logger = Logger.getLogger(FixedAccountRestService.class);
    private final KeycloakSession session;
    private final Auth auth;
    private final EventBuilder event;

    private final UserModel user;
    private final RealmModel realm;

    public FixedAccountRestService(KeycloakSession session, Auth auth, EventBuilder event) {
        this.session = session;
        this.auth = auth;
        this.event = event;
        this.user = auth.getUser();
        this.realm = auth.getRealm();
    }

    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public UserRepresentation getAccount(final @QueryParam("userProfileMetadata") Boolean userProfileMetadata) {
        var kcService = new AccountRestService(session, auth, event, AccountRestApiVersion.V1_ALPHA1);
        return kcService.account(userProfileMetadata);
    }

    @Path("/credentials")
    public FixedAccountCredentialResource credentials() {
        Cors.builder().allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().add();
        if (!Profile.isFeatureEnabled(Profile.Feature.ACCOUNT_API)) {
            throw new NotFoundException();
        }
        return new FixedAccountCredentialResource(session, user, auth, event);
    }

    /**
     * @param rep new account
     * @return REST response
     */
    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response updateAccount(UserRepresentation rep) {
        boolean emailUpdated = user != null && rep.getEmail() != null && !rep.getEmail().equalsIgnoreCase(user.getEmail());
        var kcService = new AccountRestService(session, auth, event, AccountRestApiVersion.V1_ALPHA1);
        Response resp = kcService.updateAccount(rep);
        if (emailUpdated && resp.getStatus() < 400) {
            /*
             * EmailVerified is not updatable through KC API in version 14.0
             */
            user.setEmailVerified(false);
        }

        if (user != null && resp.getStatus() / 100 == 2) {
            String representation = "";
            try {
                representation = JsonSerialization.writeValueAsString(rep);
            } catch (IOException e) {
                logger.warn("failed to serialize user representation for ACCOUNT_UPDATED event");
            }

            event.event(EventType.UPDATE_PROFILE)
                    .user(user)
                    .client(auth.getClient())
                    .detail("ct_event_type", "ACCOUNT_UPDATED")
                    .detail("username", user.getUsername())
                    .detail("representation", representation)
                    .success();
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
                    .detail("ct_event_type", "ACCOUNT_DELETED")
                    .detail("username", delUser.getUsername())
                    .success();
            return Cors.builder().auth().allowedOrigins(auth.getToken()).add(Response.noContent());
        } else {
            event.event(EventType.UPDATE_PROFILE).user(delUser)
                    .client(auth.getClient())
                    .detail("ct_event_type", "ACCOUNT_DELETED_ERROR")
                    .detail("username", delUser.getUsername())
                    .success();
            throw ErrorResponse.error("User couldn't be deleted", Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Send execute actions email
     */
    @Path("execute-actions-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response executeActionsEmail(@QueryParam("lifespan") Integer lifespan, List<String> actions) {
        if (!user.isEnabled()) {
            throw new WebApplicationException(
                    ErrorResponse.error("User is disabled", Status.BAD_REQUEST));
        }

        String emailToValidate = StringUtils.trim(user.getFirstAttribute(ExecuteActionsEmailHelper.ATTRB_EMAIL_TO_VALIDATE));
        UserModel targetUser = this.user;
        if (StringUtils.isNotBlank(emailToValidate) && actions.contains(ExecuteActionsEmailHelper.VERIFY_EMAIL_ACTION)) {
            targetUser = new CtUserModelDelegate(user);
            targetUser.setEmail(emailToValidate);
        } else if (StringUtils.isBlank(user.getEmail())) {
            throw ErrorResponse.error("User email missing", Status.BAD_REQUEST);
        }

        try {
            ExecuteActionsEmailHelper.sendExecuteActionsEmail(session, realm, targetUser, actions, lifespan, null,
                    Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, null);
            return Response.noContent().build();
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendActionsEmail(e);
            throw ErrorResponse.error("Failed to send execute actions email", Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Path("send-email")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendMail(@QueryParam("subject") String subjectFormatKey, @QueryParam("template") String template, @QueryParam("recipient") String recipient, Map<String, String> parameters) {
        if (StringUtils.isBlank(template)) {
            throw ErrorResponse.error("Template email missing", Status.BAD_REQUEST);
        }
        if (StringUtils.isBlank(subjectFormatKey)) {
            throw ErrorResponse.error("Subject missing", Status.BAD_REQUEST);
        }
        UserWithOverridenEmail userWithEmail = new UserWithOverridenEmail(user, recipient);
        if (StringUtils.isBlank(userWithEmail.getEmail())) {
            throw ErrorResponse.error("User email missing", Status.BAD_REQUEST);
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
        attributes.put("user", new ProfileBean(user, session));
        attributes.put("realmName", realm.getDisplayName());
        attributes.put("link", link);
        attributes.putAll(parameters);

        try {
            emailProvider.send(subjectFormatKey, template, attributes);
            return Response.noContent().build();
        } catch (EmailException e) {
            throw new WebApplicationException("Can't send mail to " + userWithEmail.getEmail(), e);
        }
    }
}
