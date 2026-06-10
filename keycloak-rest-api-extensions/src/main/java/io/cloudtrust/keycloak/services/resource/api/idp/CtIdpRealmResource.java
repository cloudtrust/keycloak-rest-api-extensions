package io.cloudtrust.keycloak.services.resource.api.idp;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.util.Optional;

public class CtIdpRealmResource {
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    private final String EXTIDP_TEAMMEMBER_GROUP = "extidp_teammember";

    private static final Logger logger = Logger.getLogger(CtIdpRealmResource.class);

    public CtIdpRealmResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
    }

    /**
     * Delete an external IDP team member user.
     *
     * @param userId the user ID to delete
     */
    @DELETE
    @NoCache
    @Path("team-members/{user-id}")
    public Response deleteExtIdpTeamMemberUser(@PathParam("user-id") String userId) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, userId);
        auth.users().requireManage(user);
        if (user == null) {
            throw ErrorResponse.error("User not found", Response.Status.NOT_FOUND);
        }

        if (user.getGroupsStream().noneMatch(g -> g.getName().equals(EXTIDP_TEAMMEMBER_GROUP))) {
            logger.warnf("User %s is not part of the EXTIDP team member group, deletion is not allowed", userId);
            throw ErrorResponse.error("User is not part of the EXTIDP team member group", Response.Status.BAD_REQUEST);
        }

        // We check that the user we intend to delete has a parentID and a federated identity, otherwise we fail
        String parentId = user.getFirstAttribute("parentID");
        Optional<String> idpName = session.users().getFederatedIdentitiesStream(realm, user)
                .map(FederatedIdentityModel::getIdentityProvider)
                .findFirst();
        if (StringUtils.isEmpty(parentId) || idpName.isEmpty()) {
            logger.warnf("User %s is missing a parentID or a federated identity, deletion is not allowed", userId);
            throw ErrorResponse.error("User couldn't be deleted", Response.Status.BAD_REQUEST);
        }

        boolean removed = new UserManager(session).removeUser(realm, user);
        if (!removed) {
            logger.warnf("Error during deletion of user %s", userId);
            throw ErrorResponse.error("User couldn't be deleted", Response.Status.BAD_REQUEST);
        }

        // We mimic the federated identity deletion resourcePath to make it transparent for the end users
        String resourcePath = String.format("users/%s/federated-identity/%s", user.getFirstAttribute("parentID"), idpName.get());
        adminEvent.operation(OperationType.DELETE).resourcePath(resourcePath).success();
        return Response.noContent().build();
    }
}
