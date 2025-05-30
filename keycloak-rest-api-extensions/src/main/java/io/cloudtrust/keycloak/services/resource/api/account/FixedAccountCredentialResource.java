package io.cloudtrust.keycloak.services.resource.api.account;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.credential.PasswordCredentialProviderFactory;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.messages.Messages;
import org.keycloak.utils.MediaType;

import java.util.List;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * Copy of {@link org.keycloak.services.resources.account.AccountCredentialResource} with additional resources
 */
public class FixedAccountCredentialResource {
    private final KeycloakSession session;
    private final EventBuilder event;
    private final UserModel user;
    private Auth auth;

    public FixedAccountCredentialResource(KeycloakSession session, UserModel user, Auth auth, EventBuilder event) {
        this.session = session;
        this.event = event;
        this.user = user;
        this.auth = auth;
    }

    @GET
    @NoCache
    @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    public List<CredentialRepresentation> credentials() {
        auth.requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE);
        return user.credentialManager().getStoredCredentialsStream().map(c -> {
            c.setSecretData(null);
            return ModelToRepresentation.toRepresentation(c);
        }).collect(Collectors.toList());
    }

    @GET
    @Path("registrators")
    @NoCache
    @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    public List<String> getCredentialRegistrators() {
        auth.requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE);

        return session.getContext().getRealm().getRequiredActionProvidersStream()
                .filter(RequiredActionProviderModel::isEnabled)
                .map(RequiredActionProviderModel::getProviderId)
                .filter(providerId -> session.getProvider(RequiredActionProvider.class, providerId) instanceof CredentialRegistrator)
                .toList();
    }

    /**
     * Remove a credential for a user
     */
    @Path("{credentialId}")
    @DELETE
    @NoCache
    public void removeCredential(final @PathParam("credentialId") String credentialId) {
        auth.require(AccountRoles.MANAGE_ACCOUNT);

        if (credentialNotOwnedByUser(user, credentialId)) {
            throw new NotFoundException("Credential not found");
        }

        user.credentialManager().removeStoredCredentialById(credentialId);
    }

    /**
     * Update a credential label for a user
     */
    @PUT
    @Consumes(jakarta.ws.rs.core.MediaType.TEXT_PLAIN)
    @Path("{credentialId}/label")
    public void setLabel(final @PathParam("credentialId") String credentialId, String userLabel) {
        auth.require(AccountRoles.MANAGE_ACCOUNT);

        if (credentialNotOwnedByUser(user, credentialId)) {
            throw new NotFoundException("Credential not found");
        }

        user.credentialManager().updateCredentialLabel(credentialId, userLabel);
    }

    /**
     * Move a credential to the first position
     *
     * @param credentialId The credential to move
     */
    @Path("{credentialId}/moveToFirst")
    @POST
    public Response moveToFirst(final @PathParam("credentialId") String credentialId) {
        return moveCredentialAfter(credentialId, null);
    }

    /**
     * Move a credential to a position behind another credential
     *
     * @param credentialId            The credential to move
     * @param newPreviousCredentialId The credential that will be the previous element in the list. If set to null, the moved credential will be the first element in the list.
     */
    @Path("{credentialId}/moveAfter/{newPreviousCredentialId}")
    @POST
    public Response moveCredentialAfter(final @PathParam("credentialId") String credentialId, final @PathParam("newPreviousCredentialId") String newPreviousCredentialId) {
        auth.require(AccountRoles.MANAGE_ACCOUNT);

        if (credentialNotOwnedByUser(user, credentialId)) {
            return Response.status(NOT_FOUND).build();
        }

        if (newPreviousCredentialId != null && credentialNotOwnedByUser(user, newPreviousCredentialId)) {
            return Response.status(NOT_FOUND).build();
        }

        user.credentialManager().moveStoredCredentialTo(credentialId, newPreviousCredentialId);
        return Response.noContent().build();
    }

    @GET
    @Path("password")
    @Produces(MediaType.APPLICATION_JSON)
    public PasswordDetails passwordDetails() {
        auth.requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE);

        RealmModel realm = session.getContext().getRealm();
        PasswordCredentialProvider passwordProvider = (PasswordCredentialProvider) session.getProvider(CredentialProvider.class, PasswordCredentialProviderFactory.PROVIDER_ID);
        CredentialModel password = passwordProvider.getPassword(realm, user);

        PasswordDetails details = new PasswordDetails();
        if (password != null) {
            details.setRegistered(true);
            details.setLastUpdate(password.getCreatedDate());
        } else {
            details.setRegistered(false);
        }

        return details;
    }

    @POST
    @Path("password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response passwordUpdate(PasswordUpdate update) {
        auth.require(AccountRoles.MANAGE_ACCOUNT);

        event.event(EventType.RESET_PASSWORD);

        UserCredentialModel cred = new UserCredentialModel("", PasswordCredentialModel.TYPE, update.getCurrentPassword(), false);
        if (!user.credentialManager().isValid(cred)) {
            event.error(org.keycloak.events.Errors.INVALID_USER_CREDENTIALS);
            throw ErrorResponse.error(Messages.INVALID_PASSWORD_EXISTING, Response.Status.BAD_REQUEST);
        }

        if (update.getNewPassword() == null) {
            throw ErrorResponse.error(Messages.INVALID_PASSWORD_EXISTING, Response.Status.BAD_REQUEST);
        }

        String confirmation = update.getConfirmation();
        if ((confirmation != null) && !update.getNewPassword().equals(confirmation)) {
            throw ErrorResponse.error(Messages.NOTMATCH_PASSWORD, Response.Status.BAD_REQUEST);
        }

        try {
            user.credentialManager().updateCredential(UserCredentialModel.password(update.getNewPassword(), false));
        } catch (ModelException e) {
            throw ErrorResponse.error(e.getMessage(), e.getParameters(), Response.Status.BAD_REQUEST);
        }

        return Response.noContent().build();
    }

    public static class PasswordDetails {

        private boolean registered;
        private long lastUpdate;

        public boolean isRegistered() {
            return registered;
        }

        public void setRegistered(boolean registered) {
            this.registered = registered;
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }

    }

    public static class PasswordUpdate {

        private String currentPassword;
        private String newPassword;
        private String confirmation;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getConfirmation() {
            return confirmation;
        }

        public void setConfirmation(String confirmation) {
            this.confirmation = confirmation;
        }

    }

    private boolean credentialNotOwnedByUser(UserModel user, String credentialId) {
        return user.credentialManager().getStoredCredentialsStream().filter(c -> c.getId().equals(credentialId)).count() != 1;
    }
}
