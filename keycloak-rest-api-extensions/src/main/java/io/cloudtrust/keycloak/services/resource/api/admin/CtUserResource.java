package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.ExecuteActionsEmailHelper;
import io.cloudtrust.keycloak.authentication.delegate.CtUserModelDelegate;
import io.cloudtrust.keycloak.email.EmailSender;
import io.cloudtrust.keycloak.email.model.EmailModel;
import io.cloudtrust.keycloak.email.model.RealmWithOverridenEmailTheme;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CtUserResource {
    private static final Logger logger = Logger.getLogger(CtUserResource.class);
    private static final String USER_EMAIL_MISSING = "User email missing";

    protected static final String ATTRIB_NAME_ID = "saml.persistent.name.id.for.*";

    private UserResource kcUserResource;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final KeycloakSession kcSession;
    private final UserModel user;

    public CtUserResource(KeycloakSession kcSession, UserModel user, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.kcUserResource = new UserResource(kcSession, user, auth, adminEvent);
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.kcSession = kcSession;
        this.user = user;
    }

    /**
     * Get representation of the user
     *
     * @return Requested user
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUser(@Parameter(description = "Indicates if the user profile metadata should be added to the response") @QueryParam("userProfileMetadata") boolean userProfileMetadata) {
        UserRepresentation res = kcUserResource.getUser(userProfileMetadata);
        String nameId = this.user.getFirstAttribute(ATTRIB_NAME_ID);
        if (StringUtils.isNotBlank(nameId)) {
            res.getAttributes().put(ATTRIB_NAME_ID, Collections.singletonList(nameId));
        }
        return res;
    }

    @Path("send-email")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendMail(EmailModel emailModel) {
        auth.users().requireManage();

        if (emailModel.getRecipient() == null) {
            emailModel.setRecipient(user.getEmail());
        }
        if (StringUtils.isBlank(emailModel.getRecipient())) {
            throw ErrorResponse.error(USER_EMAIL_MISSING, Response.Status.BAD_REQUEST);
        }

        if (!user.isEnabled()) {
            throw new WebApplicationException(ErrorResponse.error("User is disabled", Response.Status.BAD_REQUEST));
        }

        if (emailModel.getTheming() != null) {
            setEmailTheme(emailModel.getTheming().getThemeRealmName());
        }

        KeycloakContext context = this.kcSession.getContext();
        RealmModel realm = context.getRealm();
        Locale locale = context.resolveLocale(user);

        UriBuilder builder = LoginActionsService.loginActionsBaseUrl(context.getUri());
        String link = builder.build(realm.getName()).toString() + "/";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", new ProfileBean(user, this.kcSession));
        attributes.put("realmName", realm.getDisplayName());
        attributes.put("link", link);
        if (emailModel.getTheming() != null && emailModel.getTheming().getTemplateParameters() != null) {
            attributes.putAll(emailModel.getTheming().getTemplateParameters());
        }

        return EmailSender.sendMail(kcSession, realm, emailModel, locale, attributes);
    }

    /**
     * Send an update account email to the user
     * <p>
     * An email contains a link the user can click to perform a set of required actions.
     * The redirectUri and clientId parameters are optional. If no redirect is given, then there will
     * be no link back to click after actions have completed.  Redirect uri must be a valid uri for the
     * particular clientId.
     *
     * @param redirectUri    Redirect uri
     * @param clientId       Client id
     * @param lifespan       Number of seconds after which the generated token expires
     * @param custom1        Custom parameter
     * @param custom2        Custom parameter
     * @param custom3        Custom parameter
     * @param custom4        Custom parameter
     * @param custom5        Custom parameter
     * @param themeRealmName Name of the realm used for theming
     * @param actions        required actions the user needs to complete
     * @return status no-content if successful, returns error otherwise
     */
    @Path("execute-actions-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response executeActionsEmail(@QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri,
                                        @QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId,
                                        @QueryParam("lifespan") Integer lifespan,
                                        @QueryParam("custom1") String custom1,
                                        @QueryParam("custom2") String custom2,
                                        @QueryParam("custom3") String custom3,
                                        @QueryParam("custom4") String custom4,
                                        @QueryParam("custom5") String custom5,
                                        @QueryParam("themeRealm") String themeRealmName,
                                        List<String> actions) {
        auth.users().requireManage(user);

        if (user.getEmail() == null) {
            throw ErrorResponse.error(USER_EMAIL_MISSING, Response.Status.BAD_REQUEST);
        }

        validateUser(user);
        validateRedirectUriAndClient(redirectUri, clientId);

        if (clientId == null) {
            clientId = Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
        }

        Map<String, String> attributes = new HashMap<>();
        attributes.put("custom1", custom1);
        attributes.put("custom2", custom2);
        attributes.put("custom3", custom3);
        attributes.put("custom4", custom4);
        attributes.put("custom5", custom5);

        UserModel targetUser = this.user;
        String emailToValidate = StringUtils.trim(user.getFirstAttribute(ExecuteActionsEmailHelper.ATTRB_EMAIL_TO_VALIDATE));
        if (StringUtils.isNotBlank(emailToValidate) && actions.contains(ExecuteActionsEmailHelper.VERIFY_EMAIL_ACTION)) {
            targetUser = new CtUserModelDelegate(user);
            targetUser.setEmail(emailToValidate);
        } else if (StringUtils.isBlank(user.getEmail())) {
            throw ErrorResponse.error(USER_EMAIL_MISSING, Response.Status.BAD_REQUEST);
        }

        ClientModel client = getEnabledClientOrFail(clientId);

        String redirect;
        if (redirectUri != null) {
            redirect = RedirectUtils.verifyRedirectUri(this.kcSession, redirectUri, client);
            if (redirect == null) {
                throw new WebApplicationException(ErrorResponse.error("Invalid redirect uri.", Response.Status.BAD_REQUEST));
            }
        }

        setEmailTheme(themeRealmName);

        RealmModel realm = this.kcSession.getContext().getRealm();
        try {
            ExecuteActionsEmailHelper.sendExecuteActionsEmail(this.kcSession, realm, targetUser, actions, lifespan, redirectUri, clientId, attributes);
            adminEvent.operation(OperationType.ACTION).resourcePath(this.kcSession.getContext().getUri()).success();

            return Response.noContent().build();
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendActionsEmail(e);
            throw ErrorResponse.error("Failed to send execute actions email", Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            this.kcSession.getContext().setRealm(realm);
        }
    }

    private void validateUser(UserModel user) {
        if (!user.isEnabled()) {
            throw new WebApplicationException(ErrorResponse.error("User is disabled", Response.Status.BAD_REQUEST));
        }
    }

    private void validateRedirectUriAndClient(String redirectUri, String clientId) {
        if (redirectUri != null && clientId == null) {
            throw new WebApplicationException(ErrorResponse.error("Client id missing", Response.Status.BAD_REQUEST));
        }
    }

    private ClientModel getEnabledClientOrFail(String clientId) {
        RealmModel realm = this.kcSession.getContext().getRealm();
        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            logger.debugf("Client %s doesn't exist", clientId);
            throw new WebApplicationException(ErrorResponse.error("Client doesn't exist", Response.Status.BAD_REQUEST));
        }
        if (!client.isEnabled()) {
            logger.debugf("Client %s is not enabled", clientId);
            throw new WebApplicationException(ErrorResponse.error("Client is not enabled", Response.Status.BAD_REQUEST));
        }
        return client;
    }

    private void setEmailTheme(String themeRealmName) {
        if (!StringUtils.isBlank(themeRealmName)) {
            RealmManager realmManager = new RealmManager(this.kcSession);
            RealmModel themeRealm = realmManager.getRealmByName(themeRealmName);
            if (themeRealm == null) {
                throw ErrorResponse.error("Invalid realm name", Response.Status.BAD_REQUEST);
            }
            if (themeRealm.getEmailTheme() != null) {
                RealmWithOverridenEmailTheme overridenRealm = new RealmWithOverridenEmailTheme(this.kcSession.getContext().getRealm());
                overridenRealm.setEmailTheme(themeRealm.getEmailTheme());
                this.kcSession.getContext().setRealm(overridenRealm);
            }
        }
    }
}
