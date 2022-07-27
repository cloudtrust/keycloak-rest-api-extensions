package io.cloudtrust.keycloak.authentication.actiontoken;

import io.cloudtrust.keycloak.ExecuteActionsEmailHelper;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.TokenUtils;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Objects;

/**
 * Inspired by Keycloak ExecuteActionsActionTokenHandler
 */
public class CtExecuteActionsActionTokenHandler extends AbstractActionTokenHandler<CtExecuteActionsActionToken> {
    private static final Logger logger = Logger.getLogger(CtExecuteActionsActionTokenHandler.class);

    public CtExecuteActionsActionTokenHandler() {
        super(
                CtExecuteActionsActionToken.TOKEN_TYPE,
                CtExecuteActionsActionToken.class,
                Messages.INVALID_CODE,
                EventType.EXECUTE_ACTIONS,
                Errors.NOT_ALLOWED
        );
    }

    @Override
    public TokenVerifier.Predicate<? super CtExecuteActionsActionToken>[] getVerifiers(ActionTokenContext<CtExecuteActionsActionToken> tokenContext) {
        return TokenUtils.predicates(
                TokenUtils.checkThat(
                        // either redirect URI is not specified or must be valid for the client
                        t -> t.getRedirectUri() == null
                                || RedirectUtils.verifyRedirectUri(tokenContext.getSession(), t.getRedirectUri(),
                                tokenContext.getAuthenticationSession().getClient()) != null,
                        Errors.INVALID_REDIRECT_URI,
                        Messages.INVALID_REDIRECT_URI
                ),

                verifyEmail(tokenContext)
        );
    }

    @Override
    public Response handleToken(CtExecuteActionsActionToken token, ActionTokenContext<CtExecuteActionsActionToken> tokenContext) {
        AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();
        final UriInfo uriInfo = tokenContext.getUriInfo();
        final RealmModel realm = tokenContext.getRealm();
        final KeycloakSession session = tokenContext.getSession();
        if (tokenContext.isAuthenticationSessionFresh()) {
            // Update the authentication session in the token
            String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authSession).getEncodedId();
            token.setCompoundAuthenticationSessionId(authSessionEncodedId);
            UriBuilder builder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo),
                    authSession.getClient().getClientId(), authSession.getTabId());
            String confirmUri = builder.build(realm.getName()).toString();

            return session.getProvider(LoginFormsProvider.class)
                    .setAuthenticationSession(authSession)
                    .setSuccess(Messages.CONFIRM_EXECUTION_OF_ACTIONS)
                    .setAttribute(Constants.TEMPLATE_ATTR_ACTION_URI, confirmUri)
                    .setAttribute(Constants.TEMPLATE_ATTR_REQUIRED_ACTIONS, token.getRequiredActions())
                    .createInfoPage();
        }

        String redirectUri = RedirectUtils.verifyRedirectUri(tokenContext.getSession(), token.getRedirectUri(), authSession.getClient());
        if (redirectUri != null) {
            authSession.setAuthNote(AuthenticationManager.SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS, "true");

            authSession.setRedirectUri(redirectUri);
            authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        }

        token.getRequiredActions().forEach(authSession::addRequiredAction);

        UserModel user = tokenContext.getAuthenticationSession().getAuthenticatedUser();
        // verify user email as we know it is valid as this entry point would never have gotten here.
        if (!setEmailVerified(user, token)) {
            return session.getProvider(LoginFormsProvider.class)
                    .setError("expiredActionTokenNoSessionMessage")
                    .createErrorPage(Response.Status.NOT_FOUND);
        }

        String nextAction = AuthenticationManager.nextRequiredAction(tokenContext.getSession(), authSession, tokenContext.getRequest(), tokenContext.getEvent());
        return AuthenticationManager.redirectToRequiredActions(tokenContext.getSession(), tokenContext.getRealm(), authSession, tokenContext.getUriInfo(), nextAction);
    }

    private boolean setEmailVerified(UserModel user, CtExecuteActionsActionToken token) {
        boolean isCtVerifyEmail = token.getRequiredActions().contains(ExecuteActionsEmailHelper.VERIFY_EMAIL_ACTION);
        String emailClaim = StringUtils.defaultString(token.getEmailToValidate(), user.getEmail());
        if (StringUtils.isBlank(emailClaim)) {
            return !isCtVerifyEmail;
        }
        if (emailClaim.equalsIgnoreCase(user.getFirstAttribute(ExecuteActionsEmailHelper.ATTRB_EMAIL_TO_VALIDATE))) {
            user.setEmail(emailClaim);
            user.removeAttribute(ExecuteActionsEmailHelper.ATTRB_EMAIL_TO_VALIDATE);
            user.setEmailVerified(true);
            return true;
        }
        if (emailClaim.equalsIgnoreCase(user.getEmail())) {
            user.setEmailVerified(true);
            return true;
        }
        logger.infof("User is trying to verify a non matching email");
        return false;
    }

    @Override
    public boolean canUseTokenRepeatedly(CtExecuteActionsActionToken token, ActionTokenContext<CtExecuteActionsActionToken> tokenContext) {
        RealmModel realm = tokenContext.getRealm();
        KeycloakSessionFactory sessionFactory = tokenContext.getSession().getKeycloakSessionFactory();

        return token.getRequiredActions().stream()
                .map(realm::getRequiredActionProviderByAlias)    // get realm-specific model from action name and filter out irrelevant
                .filter(Objects::nonNull)
                .filter(RequiredActionProviderModel::isEnabled)

                .map(RequiredActionProviderModel::getProviderId)      // get provider ID from model

                .map(providerId -> (RequiredActionFactory) sessionFactory.getProviderFactory(RequiredActionProvider.class, providerId))
                .filter(Objects::nonNull)

                .noneMatch(RequiredActionFactory::isOneTimeAction);
    }
}
