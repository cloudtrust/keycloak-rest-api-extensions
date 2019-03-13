package io.cloudtrust.keycloak.services.resource.api;

import io.cloudtrust.keycloak.representations.idm.CredentialRepresentation;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;
import org.keycloak.storage.StorageId;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

public class CredentialsResource {

    private static final Logger logger = Logger.getLogger(CredentialsResource.class);

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

    @Path("{id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public CredentialRepresentation getCredential(final @PathParam("id") String id) {
        CredentialModel credential = session.userCredentialManager().getStoredCredentialById(realm, user, id);
        if (credential == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery()) throw new NotFoundException("Credential not found");
            else throw new ForbiddenException();
        }
        return toRepresentation(session.userCredentialManager().getStoredCredentialById(realm, user, id));
    }

    @Path("{id}")
    @DELETE
    public void deleteCredential(final @PathParam("id") String id) {
        auth.users().requireManage(user);
        try {
            UserCredentialStore ucs;
            if (StorageId.isLocalStorage(user)) {
                ucs = (UserCredentialStore) session.userLocalStorage();
            } else {
                ucs = (UserCredentialStore) session.userFederatedStorage();
            }
            if (ucs.removeStoredCredential(realm, user, id)) {
                adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).success();
                if (session.getTransactionManager().isActive()) {
                    session.getTransactionManager().commit();
                }
            } else {
                logger.warn("Could not delete credential " + id + " of user " + user.getUsername() +
                        "! removeStoredCredential() returned false.");
            }
        } catch (Exception me) {
            logger.warn("Could not delete credential " + id + " of user " + user.getUsername() + "!", me);
        }
    }

    public CredentialRepresentation toRepresentation(CredentialModel model) {
        CredentialRepresentation result = new CredentialRepresentation();
        result.setId(model.getId());
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
