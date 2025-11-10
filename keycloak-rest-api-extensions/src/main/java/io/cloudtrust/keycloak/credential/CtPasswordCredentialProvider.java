package io.cloudtrust.keycloak.credential;

import io.cloudtrust.keycloak.Events;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import java.util.Map;

public class CtPasswordCredentialProvider extends PasswordCredentialProvider {

    private static final Logger logger = Logger.getLogger(CtPasswordCredentialProvider.class);

    public CtPasswordCredentialProvider(KeycloakSession session, Meter.MeterProvider<Counter> meterProvider, boolean metricsEnabled,
                                        boolean withRealmInMetric, boolean withAlgorithmInMetric, boolean withHashingStrengthInMetric, boolean withOutcomeInMetric) {
        super(session, meterProvider, metricsEnabled, withRealmInMetric, withAlgorithmInMetric, withHashingStrengthInMetric, withOutcomeInMetric);
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, PasswordCredentialModel credentialModel) {
        CredentialModel createdCredential = super.createCredential(realm, user, credentialModel);
        emitEvent(realm, user, createdCredential.getId());
        return createdCredential;
    }

    private void emitEvent(RealmModel realm, UserModel user, String credentialId) {
        if (session.getContext().getRequestHeaders() == null) {
            logger.warn("no http request header, cannot have enough information to emit event for password creation, realm=" + realm.getName() + ", userId=" + user.getId());
            return;
        }

        String token = AppAuthManager.extractAuthorizationHeaderTokenOrReturnNull(session.getContext().getRequestHeaders());
        if (StringUtils.isBlank(token)) {
            emitUpdatePasswordEvent(realm, user, credentialId);
            return;
        }

        AccessToken accessToken;
        try {
            accessToken = TokenVerifier.create(token, AccessToken.class).parse().getToken();
        } catch (VerificationException e) {
            logger.warn("failed to get token, no event emitted for password creation, realm=" + realm.getName() + ", userId=" + user.getId());
            return;
        }

        String agentUserId = accessToken.getSubject();
        String agentClientId = accessToken.getIssuedFor();
        String issuer = accessToken.getIssuer();
        String agentRealmName = issuer.substring(issuer.lastIndexOf("/") + 1);
        RealmModel agentRealm = session.realms().getRealmByName(agentRealmName);

        if (agentRealm == null || agentUserId == null || agentRealm.getId().equals(realm.getId()) && agentUserId.equals(user.getId())) {
            // self password creation or reset (or token does not contain agent information)
            emitUpdatePasswordEvent(realm, user, credentialId);
        } else {
            // password created or reset by another user (agent)
            UserModel agent = session.users().getUserById(agentRealm, agentUserId);
            ClientModel agentClient = session.clients().getClientByClientId(agentRealm, agentClientId);

            AdminAuth adminAuth = new AdminAuth(agentRealm, accessToken, agent, agentClient);
            AdminEventBuilder adminEventBuilder = new AdminEventBuilder(realm, adminAuth, session, session.getContext().getConnection());
            adminEventBuilder.operation(OperationType.CREATE)
                    .resource(ResourceType.CUSTOM)
                    .resourcePath("users", user.getId())
                    .representation(Map.of(Events.CT_EVENT_CREDENTIAL_ID, credentialId, "credential_type", "password"))
                    .success();
        }
    }

    private void emitUpdatePasswordEvent(RealmModel realm, UserModel user, String credentialId) {
        EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
        eventBuilder.event(EventType.CUSTOM_REQUIRED_ACTION)
                .user(user)
                .detail(Events.CT_EVENT_TYPE, EventType.RESET_PASSWORD.toString())
                .detail(Events.CT_EVENT_CREDENTIAL_ID, credentialId)
                .success();
    }
}
