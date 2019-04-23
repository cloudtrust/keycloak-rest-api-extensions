package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.services.NotImplementedException;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UsersResource extends org.keycloak.services.resources.admin.UsersResource {

    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;
    private KeycloakSession session;

    public UsersResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        super(session.getContext().getRealm(), auth, adminEvent);
        this.auth = auth;
        this.session = session;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
    }

    /**
     * Get representation of the user
     *
     * @param id User id
     * @return
     */
    @Path("{id}")
    public org.keycloak.services.resources.admin.UserResource user(final @PathParam("id") String id) {
        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery()) throw new NotFoundException("User not found");
            else throw new ForbiddenException();
        }
        UserResource resource = new UserResource(session, user, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    /**
     * Get users
     *
     * Returns a list of users, filtered according to query parameters.
     * Differs from the standard getUsers by being able to filter on groups or roles
     * Adding multiple groups or multiple roles returns a union of all users that fit those groups or roles
     * Adding multiple groups and multiple roles will give the intersection of users that belong both to the the specified groups and the specified roles
     * If either groups or roles are specified in the call, paging with "first" and/or "max" is impossible, and trying will return a 501.
     *
     * @param groups A list of group Ids
     * @param roles A list of realm role names
     * @param last A user's last name
     * @param first A user's first name
     * @param email A user's email
     * @param username A user's username
     * @param first Pagination offset
     * @param maxResults Maximum results size (defaults to 100)
     * @return A list of users corresponding to the searched parameters
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserRepresentation> getUsers(@QueryParam("groupId") List<String> groups,
                                             @QueryParam("roleName") List<String> roles,
                                             @QueryParam("lastName") String last,
                                             @QueryParam("firstName") String first,
                                             @QueryParam("email") String email,
                                             @QueryParam("username") String username,
                                             @QueryParam("first") Integer firstResult,
                                             @QueryParam("max") Integer maxResults,
                                             @QueryParam("briefRepresentation") Boolean briefRepresentation) {
        auth.users().requireView();
        RealmModel realm = session.getContext().getRealm();
        if (roles == null || roles.isEmpty() && (groups == null || groups.isEmpty())) {
            return getUsers(null, last, first, email, username, firstResult, maxResults, briefRepresentation);
        }
        if (firstResult != null || maxResults != null) {
            throw new NotImplementedException();
        }
        List<UserRepresentation> tempUsers = getUsers(null, last, first, email, username, firstResult, maxResults, briefRepresentation);
        Set<String> usersWithGroup = new HashSet<>();
        if (groups != null && !groups.isEmpty()) {
            this.auth.groups().requireView();
            groups.stream().filter(group -> realm.getGroupById(group) != null).flatMap(group -> session.users().getGroupMembers(realm, realm.getGroupById(group)).stream()).forEach(user -> usersWithGroup.add(user.getId()));
            tempUsers = tempUsers.stream().filter(user -> usersWithGroup.contains(user.getId())).collect(Collectors.toList());
        }
        Set<String> usersWithRole = new HashSet<>();
        if (roles != null && !roles.isEmpty()) {
            auth.roles().requireView(session.getContext().getRealm());
            roles.stream().filter(role -> realm.getRole(role) != null).flatMap(role -> session.users().getRoleMembers(realm, realm.getRole(role)).stream()).forEach(user -> usersWithRole.add(user.getId()));
            tempUsers = tempUsers.stream().filter(user -> usersWithRole.contains(user.getId())).collect(Collectors.toList());
        }
        return tempUsers;
    }
}
