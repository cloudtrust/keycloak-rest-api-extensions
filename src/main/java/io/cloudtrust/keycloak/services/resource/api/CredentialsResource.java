package io.cloudtrust.keycloak.services.resource.api;

import io.cloudtrust.keycloak.representations.idm.CredentialRepresentation;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.credential.UserCredentialStoreManager;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;
import org.keycloak.storage.StorageId;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    public Response deleteCredential(final @PathParam("id") String id) {
        auth.users().requireManage(user);
        try {
            // We remove the credential. In case of success...
            if (session.userCredentialManager().removeStoredCredential(realm, user, id)) {
                // We log the action
                adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).success();
                // We evict the user from the cache (or else he can still use his credential even if it's gone!)
                UserCache userCache = session.userCache();
                if (userCache != null) {
                    userCache.evict(realm, user);
                }
                // We commit the transaction
                if (session.getTransactionManager().isActive()) {
                    session.getTransactionManager().commit();
                }
                // We return an empty response
                return Response.noContent().build();
            } else {
                logger.warn("Could not delete credential " + id + " of user " + user.getUsername() +
                        "! removeStoredCredential() returned false.");
            }
        } catch (Exception e) {
            logger.warn("Could not delete credential " + id + " of user " + user.getUsername() + "!", e);
        }
        return ErrorResponse.exists("Could not delete credential!");
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
