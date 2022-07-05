package io.cloudtrust.keycloak.authentication.requiredactions;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.cloudtrust.keycloak.ExecuteActionsEmailHelper;
import io.cloudtrust.keycloak.email.model.UserWithOverridenEmail;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.DefaultActionToken;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilderException;
import java.util.Collections;
import java.util.Objects;

/**
 * Inspired by Keycloak VerifyEmail
 */
public class VerifyEmailRequiredAction implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    private static final Logger logger = Logger.getLogger(VerifyEmailRequiredAction.class);

    private static final String REQUIRED_ACTION_ID = "ct-verify-email";

    static class CtVerifyEmailActionToken extends DefaultActionToken {
        private static final String JSON_FIELD_ORIGINAL_AUTHENTICATION_SESSION_ID = "oasid";

        @JsonProperty(value = JSON_FIELD_ORIGINAL_AUTHENTICATION_SESSION_ID)
        private String originalAuthenticationSessionId;

        public CtVerifyEmailActionToken(String userId, int absoluteExpirationInSecs, String compoundAuthenticationSessionId, String email, String clientId) {
            super(userId, REQUIRED_ACTION_ID, absoluteExpirationInSecs, null, compoundAuthenticationSessionId);
            setEmail(email);
            this.issuedFor = clientId;
        }

        private CtVerifyEmailActionToken() {
        }

        public String getCompoundOriginalAuthenticationSessionId() {
            return originalAuthenticationSessionId;
        }

        public void setCompoundOriginalAuthenticationSessionId(String originalAuthenticationSessionId) {
            this.originalAuthenticationSessionId = originalAuthenticationSessionId;
        }
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (!context.getRealm().isVerifyEmail()) {
            return;
        }
        UserModel user = context.getUser();
        if (!isEmailVerified(user)) {
            user.addRequiredAction(REQUIRED_ACTION_ID);
            logger.debug("User is required to verify email (TrustID)");
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        UserModel user = context.getUser();

        if (isEmailVerified(user)) {
            context.success();
            authSession.removeAuthNote(Constants.VERIFY_EMAIL_KEY);
            return;
        }

        String email = user.getFirstAttribute("emailToValidate");
        if (StringUtils.isNotBlank(email)) {
            user = new UserWithOverridenEmail(user, email);
        }
        email = user.getEmail();
        if (StringUtils.isBlank(email)) {
            context.ignore();
            return;
        }

        authSession.setClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW, null);

        // Do not allow resending e-mail by simple page refresh, i.e. when e-mail sent, it should be resent properly via email-verification endpoint
        if (!Objects.equals(authSession.getAuthNote(Constants.VERIFY_EMAIL_KEY), email)) {
            authSession.setAuthNote(Constants.VERIFY_EMAIL_KEY, email);
            EventBuilder event = context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, email);
            sendVerifyEmail(context.getSession(), user, context.getAuthenticationSession(), event);
        }

        LoginFormsProvider loginFormsProvider = context.form()
                .setAttribute("user", new ProfileBean(user));
        Response challenge = loginFormsProvider.createResponse(UserModel.RequiredAction.VERIFY_EMAIL);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to do
    }

    private void sendVerifyEmail(KeycloakSession session, UserModel user, AuthenticationSessionModel authSession, EventBuilder event) throws UriBuilderException, IllegalArgumentException {
        try {
            ExecuteActionsEmailHelper.sendExecuteActionsEmail(session, session.getContext().getRealm(), user,
                    Collections.singletonList(REQUIRED_ACTION_ID), null, null, null, null);
            event.success();
        } catch (EmailException e) {
            logger.error("Failed to send verification email", e);
            event.error(Errors.EMAIL_SEND_FAILED);
        }
    }

    private boolean isEmailVerified(UserModel user) {
        return user.isEmailVerified() && StringUtils.isBlank(user.getFirstAttribute("emailToValidate"));
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public void init(Scope config) {
        // nothing to do
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // nothing to do
    }

    @Override
    public String getId() {
        return REQUIRED_ACTION_ID;
    }

    @Override
    public String getDisplayText() {
        return "Verify Email (TrustID)";
    }
}
