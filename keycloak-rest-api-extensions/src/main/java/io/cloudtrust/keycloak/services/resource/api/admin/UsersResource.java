package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.representations.idm.UsersPageRepresentation;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    @Override
    public Response createUser(final UserRepresentation rep) {
        // Security checks
        auth.users().requireManage();

        List<GroupModel> groups = new ArrayList<>();
        List<RoleModel> roles = new ArrayList<>();

        if (rep.getGroups() != null) {
            for (String groupId : rep.getGroups()) {
                GroupModel group = session.realms().getGroupById(groupId, realm);

                if (group == null) {
                    throw new NotFoundException("Group not found");
                }

                auth.groups().requireManageMembership(group);
                groups.add(group);
            }
        }

        if (rep.getRealmRoles() != null) {
            for (String roleId : rep.getRealmRoles()) {
                RoleModel role = realm.getRoleById(roleId);

                if (role == null) {
                    throw new NotFoundException("Role not found");
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
    @Override
    public org.keycloak.services.resources.admin.UserResource user(final @PathParam("id") String id) {
        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            // we do this to make sure nobody can phish ids
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
     * @param search     A special search field: when used, all other search fields are ignored. Can be something like id:abcd-efg-123
     *                   or a string which can be contained in the first+last name, email or username
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
        GetUsersQuery qry = new GetUsersQuery(session, auth);

        if (search != null && !search.isEmpty()) {
            qry.addPredicateSearchGlobal(search);
        } else {
            qry.addPredicateSearchFields(last, first, email, username);
        }

        qry.addPredicateForGroups(groups);
        qry.addPredicateForRoles(roles);

        qry.applyPredicates();

        int count = qry.getTotalCount();
        List<UserModel> results = qry.execute(firstResult, maxResults);
        return new UsersPageRepresentation(toRepresentation(realm, auth.users(), briefRepresentation, results), count);
    }

    /**
     * Source: keycloak-services/src/main/java/org.keycloak.services.resources.admin.UsersResource
     */
    private List<UserRepresentation> toRepresentation(RealmModel realm, UserPermissionEvaluator usersEvaluator, Boolean briefRepresentation, List<UserModel> userModels) {
        boolean briefRepresentationB = briefRepresentation != null && briefRepresentation;
        List<UserRepresentation> results = new ArrayList<>();
        boolean canViewGlobal = usersEvaluator.canView();

        usersEvaluator.grantIfNoPermission(session.getAttribute(UserModel.GROUPS) != null);

        for (UserModel user : userModels) {
            if (!canViewGlobal) {
                if (!usersEvaluator.canView(user)) {
                    continue;
                }
            }
            UserRepresentation userRep = briefRepresentationB
                    ? ModelToRepresentation.toBriefRepresentation(user)
                    : ModelToRepresentation.toRepresentation(session, realm, user);
            userRep.setAccess(usersEvaluator.getAccess(user));
            results.add(userRep);
        }
        return results;
    }
}
