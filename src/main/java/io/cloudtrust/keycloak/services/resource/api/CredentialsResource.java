package io.cloudtrust.keycloak.services.resource.api;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

public class CredentialsResource {

    private final RealmModel realm;
    private final UserModel user;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final KeycloakSession session;

    public CredentialsResource(KeycloakSession session, UserModel user, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.adminEvent = adminEvent;
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.auth = auth;
        this.user = user;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<CredentialRepresentation> getCredentials() {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireQuery();

        return session.userCredentialManager().getStoredCredentials(realm, user).stream().map(this::toRepresentation)
                .collect(Collectors.toList());
    }

    public CredentialRepresentation toRepresentation(CredentialModel model) {
        CredentialRepresentation result = new CredentialRepresentation();
        result.setDevice(model.getDevice());
        result.setAlgorithm(model.getAlgorithm());
        result.setConfig(model.getConfig());
        result.setType(model.getType());
        result.setCounter(model.getCounter());
        result.setCreatedDate(model.getCreatedDate());
        result.setDigits(model.getDigits());
        result.setHashIterations(model.getHashIterations());
        result.setPeriod(model.getPeriod());
        return result;
    }
}
