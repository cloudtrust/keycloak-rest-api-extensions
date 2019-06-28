package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.representations.idm.UsersPageRepresentation;
import io.cloudtrust.keycloak.services.NotImplementedException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

public class UsersResource extends org.keycloak.services.resources.admin.UsersResource {

    private static final Logger logger = Logger.getLogger(UsersResource.class);

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
     * Create a new user.
     * This extended API allows to assign groups and roles at user creation.
     * <p>
     * Username must be unique.
     *
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUser(final UserRepresentation rep) {
        // Security checks
        auth.users().requireManage();

        List<GroupModel> groups = new ArrayList<>();
        List<RoleModel> roles = new ArrayList<>();

        if (rep.getGroups() != null) {
            for (String groupId : rep.getGroups()) {
                GroupModel group = session.realms().getGroupById(groupId, realm);

                if (group == null) {
                    throw new org.jboss.resteasy.spi.NotFoundException("Group not found");
                }

                auth.groups().requireManageMembership(group);
                groups.add(group);
            }
        }

        if (rep.getRealmRoles() != null) {
            for (String roleId : rep.getRealmRoles()) {
                RoleModel role = realm.getRoleById(roleId);

                if (role == null) {
                    throw new org.jboss.resteasy.spi.NotFoundException("Role not found");
                }

                auth.roles().requireMapRole(role);
                roles.add(role);
            }
        }

        // Double-check duplicated username and email here due to federation
        if (session.users().getUserByUsername(rep.getUsername(), realm) != null) {
            return ErrorResponse.exists("User exists with same username");
        }
        if (rep.getEmail() != null && !realm.isDuplicateEmailsAllowed() && session.users().getUserByEmail(rep.getEmail(), realm) != null) {
            return ErrorResponse.exists("User exists with same email");
        }

        try {
            UserModel user = session.users().addUser(realm, rep.getUsername());
            Set<String> emptySet = Collections.emptySet();

            org.keycloak.services.resources.admin.UserResource.updateUserFromRep(user, rep, emptySet, realm, session, false);
            RepresentationToModel.createCredentials(rep, session, realm, user, true);

            // Add groups
            for (GroupModel group : groups) {
                user.joinGroup(group);
            }

            // Add roles
            for (RoleModel role : roles) {
                user.grantRole(role);
            }

            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), user.getId()).representation(rep).success();

            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().commit();
            }

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(user.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            return ErrorResponse.exists("User exists with same username or email");
        } catch (ModelException me) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            logger.warn("Could not create user", me);
            return ErrorResponse.exists("Could not create user");
        }
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
     * <p>
     * Returns a list of users, filtered according to query parameters.
     * Differs from the standard getUsers by being able to filter on groups or roles
     * Adding multiple groups or multiple roles returns a union of all users that fit those groups or roles
     * Adding multiple groups and multiple roles will give the intersection of users that belong both to the the specified groups and the specified roles
     * If either groups or roles are specified in the call, paging with "first" and/or "max" is impossible, and trying will return a 501.
     *
     * @param groups     A list of group Ids
     * @param roles      A list of realm role names
     * @param last       A user's last name
     * @param first      A user's first name
     * @param email      A user's email
     * @param username   A user's username
     * @param first      Pagination offset
     * @param maxResults Maximum results size (defaults to 100) - only taken into account if no group / role is defined
     * @return A list of users corresponding to the searched parameters, as well as the total count of users
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UsersPageRepresentation getUsers(@QueryParam("groupId") List<String> groups,
                                             @QueryParam("roleId") List<String> roles,
                                             @QueryParam("search") String search,
                                             @QueryParam("lastName") String last,
                                             @QueryParam("firstName") String first,
                                             @QueryParam("email") String email,
                                             @QueryParam("username") String username,
                                             @QueryParam("first") Integer firstResult,
                                             @QueryParam("max") Integer maxResults,
                                             @QueryParam("briefRepresentation") Boolean briefRepresentation) {
        auth.users().requireView();
        RealmModel realm = session.getContext().getRealm();

        // for the next call, we set firstResults to 0 and maxResults to INT_MAX for two reasons :
        // - We want to know the total count of results
        // - We want to have "max" results, even after group/role filtering
        List<UserRepresentation> tempUsers = getUsers(search, last, first, email, username, 0, Integer.MAX_VALUE, briefRepresentation);
        Set<String> usersWithGroup = new HashSet<>();
        if (groups != null && !groups.isEmpty()) {
            this.auth.groups().requireView();
            groups.stream().filter(group -> realm.getGroupById(group) != null).flatMap(group -> session.users().getGroupMembers(realm, realm.getGroupById(group)).stream()).forEach(user -> usersWithGroup.add(user.getId()));
            tempUsers = tempUsers.stream().filter(user -> usersWithGroup.contains(user.getId())).collect(Collectors.toList());
        }

        Set<String> usersWithRole = new HashSet<>();
        if (roles != null && !roles.isEmpty()) {
            auth.roles().requireView(session.getContext().getRealm());
            roles.stream().filter(role -> realm.getRoleById(role) != null).flatMap(role -> session.users().getRoleMembers(realm, realm.getRoleById(role)).stream()).forEach(user -> usersWithRole.add(user.getId()));
            tempUsers = tempUsers.stream().filter(user -> usersWithRole.contains(user.getId())).collect(Collectors.toList());
        }

        // We paginate our results
        int totalCount = tempUsers.size();
        if (firstResult != null && maxResults != null) {
            tempUsers = tempUsers.subList(firstResult, Math.min(firstResult + maxResults, totalCount));
        }

        return new UsersPageRepresentation(tempUsers, totalCount);
    }

}
