package io.cloudtrust.keycloak;

import io.cloudtrust.keycloak.authentication.actiontoken.CtExecuteActionsActionToken;
import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.LoginActionsService;

import javax.ws.rs.core.UriBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UserUtils {
    private static final Logger logger = Logger.getLogger(UserUtils.class);

    public static final String VERIFY_EMAIL_ACTION = "ct-verify-email";
    public static final String ATTRB_EMAIL_TO_VALIDATE = "emailToValidate";

    public static void sendExecuteActionsEmail(KeycloakSession session, UserModel user, List<String> actions, Integer lifespan, String clientId) throws EmailException {
        RealmModel realm = session.getContext().getRealm();
        sendExecuteActionsEmail(session, realm, user, actions, lifespan, clientId, null);
    }

    public static void sendExecuteActionsEmail(KeycloakSession session, UserModel user, List<String> actions, Integer lifespan, String clientId, Map<String, String> attributes) throws EmailException {
        RealmModel realm = session.getContext().getRealm();
        sendExecuteActionsEmail(session, realm, user, actions, lifespan, null, clientId, attributes);
    }

    public static void sendExecuteActionsEmail(KeycloakSession session, RealmModel realm, UserModel user, List<String> actions, Integer lifespan, String clientId, Map<String, String> attributes) throws EmailException {
        sendExecuteActionsEmail(session, realm, user, actions, lifespan, null, clientId, attributes);
    }

    public static void sendExecuteActionsEmail(KeycloakSession session, RealmModel realm, UserModel user, List<String> actions, Integer lifespan, String redirectUri, String clientId, Map<String, String> attributes) throws EmailException {
        if (lifespan == null) {
            lifespan = realm.getActionTokenGeneratedByAdminLifespan();
        }
        if (clientId==null) {
            clientId = Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
        }
        int expiration = Time.currentTime() + lifespan;
        CtExecuteActionsActionToken token = new CtExecuteActionsActionToken(user.getId(), expiration, actions, redirectUri, clientId);
        addClaims(user, actions, token);

        UriBuilder builder = LoginActionsService.actionTokenProcessor(session.getContext().getUri());
        builder.queryParam("key", token.serialize(session, realm, session.getContext().getUri()));

        String link = builder.build(realm.getName()).toString();

        EmailTemplateProvider emailTemplateProv = session.getProvider(EmailTemplateProvider.class)
                .setAttribute(Constants.TEMPLATE_ATTR_REQUIRED_ACTIONS, token.getRequiredActions());
        if (attributes!=null) {
            attributes.forEach(emailTemplateProv::setAttribute);
        }
        emailTemplateProv
                .setRealm(realm)
                .setUser(user)
                .sendExecuteActions(link, TimeUnit.SECONDS.toMinutes(lifespan));
    }

    private static void addClaims(UserModel user, List<String> actions, CtExecuteActionsActionToken token) {
        /* email to validate */
        if (actions.contains(VERIFY_EMAIL_ACTION)) {
            String email = StringUtils.defaultIfBlank(user.getFirstAttribute(ATTRB_EMAIL_TO_VALIDATE), user.getEmail());
            if (StringUtils.isNotBlank(email)) {
                logger.debugf("Adding email to validate to claim: %s", email);
                token.setEmailToValidate(email);
            }
        }
    }
}
