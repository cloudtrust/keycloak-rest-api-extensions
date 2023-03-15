package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.representations.idm.UsersPageRepresentation;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.policy.PasswordPolicyNotMetException;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.utils.SearchQueryUtils;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.userprofile.UserProfileContext.USER_API;

public class UsersResource extends org.keycloak.services.resources.admin.UsersResource {
    private static final Logger logger = Logger.getLogger(UsersResource.class);

    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final KeycloakSession kcSession;

    public UsersResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        super(session.getContext().getRealm(), auth, adminEvent);
        this.auth = auth;
        this.kcSession = session;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
    }

    /**
     * Create a new user.
     * This extended API allows to assign groups and roles at user creation.
     * <p>
     * Username must be unique.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/legacy")
    @Override
    public Response createUser(final UserRepresentation rep) {
        return this.createUserWithParams(rep, false);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUserWithParams(final UserRepresentation rep, final @QueryParam("generateNameID") Boolean generateNameID) {
        return legacyCreateUser(rep, user -> {
            List<GroupModel> groups = getGroups(rep.getGroups());
            List<RoleModel> roles = getRoles(rep.getRealmRoles());

            // Add groups
            groups.forEach(user::joinGroup);

            // Add roles
            roles.forEach(user::grantRole);

            if (BooleanUtils.isTrue(generateNameID)) {
                user.setSingleAttribute(CtUserResource.ATTRIB_NAME_ID, "G-" + UUID.randomUUID());
            }
        });
    }

    /**
     * Create a new user
     * <p>
     * Username must be unique.
     * This method is a copy/paste of org.keycloak.services.resources.admin.UsersResource where:
     * - we add a consumer processing
     * - we removed call to RepresentationToModel.createGroups
     *
     * @param rep         User
     * @param userUpdater Callback used to update an user
     * @return Response
     */
    private Response legacyCreateUser(final UserRepresentation rep, Consumer<UserModel> userUpdater) {
        // first check if user has manage rights
        try {
            auth.users().requireManage();
        } catch (ForbiddenException exception) {
            // if user does not have manage rights, fallback to fine grain admin permissions per group
            if (rep.getGroups() != null) {
                // if groups is part of the user rep, check if admin has manage_members and manage_membership on each group
                for (String groupPath : rep.getGroups()) {
                    GroupModel group = KeycloakModelUtils.findGroupByPath(realm, groupPath);
                    if (group != null) {
                        auth.groups().requireManageMembers(group);
                        auth.groups().requireManageMembership(group);
                    } else {
                        return ErrorResponse.error(String.format("Group %s not found", groupPath), Response.Status.BAD_REQUEST);
                    }
                }
            } else {
                // propagate exception if no group specified
                throw exception;
            }
        }

        String username = rep.getUsername();
        if (realm.isRegistrationEmailAsUsername()) {
            username = rep.getEmail();
        }
        if (ObjectUtil.isBlank(username)) {
            return ErrorResponse.error("User name is missing", Response.Status.BAD_REQUEST);
        }

        // Double-check duplicated username and email here due to federation
        if (session.users().getUserByUsername(realm, username) != null) {
            return ErrorResponse.exists("User exists with same username");
        }
        if (rep.getEmail() != null && !realm.isDuplicateEmailsAllowed()) {
            try {
                if (session.users().getUserByEmail(realm, rep.getEmail()) != null) {
                    return ErrorResponse.exists("User exists with same email");
                }
            } catch (ModelDuplicateException e) {
                return ErrorResponse.exists("User exists with same email");
            }
        }

        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);

        UserProfile profile = profileProvider.create(USER_API, rep.toAttributes());

        try {
            Response response = UserResource.validateUserProfile(profile, null, session);
            if (response != null) {
                return response;
            }

            UserModel user = profile.create();

            UserResource.updateUserFromRep(profile, user, rep, session, false);
            RepresentationToModel.createFederatedIdentities(rep, session, realm, user);

            RepresentationToModel.createCredentials(rep, session, realm, user, true);
            /* Cloudtrust specific - begin */
            userUpdater.accept(user);
            /* Cloudtrust specific - end */
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
        } catch (PasswordPolicyNotMetException e) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            return ErrorResponse.error("Password policy not met", Response.Status.BAD_REQUEST);
        } catch (ModelException me) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            logger.warn("Could not create user", me);
            return ErrorResponse.error("Could not create user", Response.Status.BAD_REQUEST);
        }
    }

    private List<GroupModel> getGroups(List<String> groups) {
        List<GroupModel> res = new ArrayList<>();
        if (groups != null) {
            for (String groupId : groups) {
                GroupModel group = kcSession.realms().getGroupsStream(realm).filter(g -> groupId.equals(g.getId())).findFirst().orElse(null);
                if (group == null) {
                    throw new NotFoundException("Group not found");
                }

                auth.groups().requireManageMembership(group);
                res.add(group);
            }
        }
        return res;
    }

    private List<RoleModel> getRoles(List<String> roles) {
        List<RoleModel> res = new ArrayList<>();
        if (roles != null) {
            for (String roleId : roles) {
                RoleModel role = realm.getRoleById(roleId);

                if (role == null) {
                    throw new NotFoundException("Role not found");
                }

                auth.roles().requireMapRole(role);
                res.add(role);
            }
        }
        return res;
    }

    /**
     * Get representation of the user
     *
     * @param id User id
     * @return Found user resource
     */
    @Path("{id}")
    @Override
    public CtUserResource user(final @PathParam("id") String id) {
        UserModel user = kcSession.users().getUserById(realm, id);
        if (user == null) {
            // we do this to make sure nobody can phish ids
            if (auth.users().canQuery()) throw new NotFoundException("User not found");
            else throw new ForbiddenException();
        }
        CtUserResource resource = new CtUserResource(kcSession, user, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    // Comes from Keycloak file UsersResource.java
    private static final String SEARCH_ID_PARAMETER = "id:";

    /*
    // For a later use, this method let the caller only get a count of matching users
    private int getUsersCount(@QueryParam("groupId") List<String> groups,
                              @QueryParam("roleId") List<String> roles,
                              @QueryParam("search") String search,
                              @QueryParam("lastName") String lastName,
                              @QueryParam("firstName") String firstName,
                              @QueryParam("email") String email,
                              @QueryParam("username") String username,
                              @QueryParam("emailVerified") Boolean emailVerified,
                              @QueryParam("enabled") Boolean enabled,
                              @QueryParam("exact") Boolean exact,
                              @QueryParam("q") String searchQuery) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireQuery();

        Map<String, String> searchAttributes = searchQuery == null
                ? Collections.emptyMap()
                : SearchQueryUtils.getFields(searchQuery);

        if (!CollectionUtil.isEmpty(groups)) {
            session.setAttribute(UserModel.GROUPS, new HashSet<>(groups));
        }
        if (!CollectionUtil.isEmpty(roles)) {
            session.setAttribute("filterRoles", new HashSet<>(roles));
        }

        if (search != null) {
            if (search.startsWith(SEARCH_ID_PARAMETER)) {
                UserModel userModel = session.users().getUserById(realm, search.substring(SEARCH_ID_PARAMETER.length()).trim());
                if (userModel != null) {
                    return 1;
                }
            } else {
                Map<String, String> attributes = new HashMap<>();
                attributes.put(UserModel.SEARCH, search.trim());
                if (enabled != null) {
                    attributes.put(UserModel.ENABLED, enabled.toString());
                }
                session.setAttribute(UserModel.INCLUDE_SERVICE_ACCOUNT, false);
                return GetUsersQuery.countUsers(session, realm, attributes);
            }
        } else if (lastName != null || firstName != null || email != null || username != null || emailVerified != null
                || enabled != null || !searchAttributes.isEmpty()) {
            Map<String, String> attributes = new HashMap<>();
            addWhenNotBlank(attributes, UserModel.LAST_NAME, lastName);
            addWhenNotBlank(attributes, UserModel.FIRST_NAME, firstName);
            addWhenNotBlank(attributes, UserModel.EMAIL, email);
            addWhenNotBlank(attributes, UserModel.USERNAME, username);
            addWhenNotBlank(attributes, UserModel.EMAIL_VERIFIED, emailVerified == null ? null : emailVerified.toString());
            addWhenNotBlank(attributes, UserModel.ENABLED, enabled == null ? null : enabled.toString());

            attributes.putAll(searchAttributes);
            session.setAttribute(UserModel.INCLUDE_SERVICE_ACCOUNT, true);
            return GetUsersQuery.countUsers(session, realm, attributes);
        } else {
            session.setAttribute(UserModel.INCLUDE_SERVICE_ACCOUNT, false);
            return GetUsersQuery.countUsers(session, realm, new HashMap<>());
        }
        return 0;
    }
    */

    /**
     * Get users
     * <p>
     * Returns a list of users, filtered according to query parameters.
     * Differs from the standard getUsers by being able to filter on groups or roles
     * Adding multiple groups or multiple roles returns a union of all users that fit those groups or roles
     * Adding multiple groups and multiple roles will give the intersection of users that belong both to the the specified groups and the specified roles
     * If either groups or roles are specified in the call, paging with "first" and/or "max" is impossible, and trying will return a 501.
     *
     * @param groups              A list of group Ids
     * @param roles               A list of realm role names
     * @param search              A special search field: when used, all other search fields are ignored. Can be something like id:abcd-efg-123
     *                            or a string which can be contained in the first+last name, email or username
     * @param lastName            A user's last name
     * @param firstName           A user's first name
     * @param email               A user's email
     * @param username            A user's username
     * @param emailVerified       Shall the email be verified or not?
     * @param firstResult         Pagination offset
     * @param maxResults          Maximum results size (defaults to 100) - only taken into account if no group / role is defined
     * @param briefRepresentation Should the API return a brief representation or the standard one
     * @param exact               Boolean which defines whether the params "last", "first", "email" and "username" must match exactly
     * @param searchQuery         A query to search for custom attributes, in the format 'key1:value2 key2:value2'
     * @return A list of users corresponding to the searched parameters, as well as the total count of users
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UsersPageRepresentation getUsers(@QueryParam("groupId") List<String> groups,
                                            @QueryParam("roleId") List<String> roles,
                                            @QueryParam("search") String search,
                                            @QueryParam("lastName") String lastName,
                                            @QueryParam("firstName") String firstName,
                                            @QueryParam("email") String email,
                                            @QueryParam("username") String username,
                                            @QueryParam("emailVerified") Boolean emailVerified,
                                            @QueryParam("first") Integer firstResult,
                                            @QueryParam("max") Integer maxResults,
                                            @QueryParam("enabled") Boolean enabled,
                                            @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                            @QueryParam("exact") Boolean exact,
                                            @QueryParam("q") String searchQuery) {
        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireQuery();

        Map<String, String> searchAttributes = searchQuery == null
                ? Collections.emptyMap()
                : SearchQueryUtils.getFields(searchQuery);

        if (!CollectionUtil.isEmpty(groups)) {
            session.setAttribute(UserModel.GROUPS, new HashSet<>(groups));
        }
        if (!CollectionUtil.isEmpty(roles)) {
            session.setAttribute("filterRoles", new HashSet<>(roles));
        }

        Stream<UserModel> userModels = Stream.empty();
        int count = 0;
        if (search != null) {
            if (search.startsWith(SEARCH_ID_PARAMETER)) {
                UserModel userModel = session.users().getUserById(realm, search.substring(SEARCH_ID_PARAMETER.length()).trim());
                if (userModel != null) {
                    count = 1;
                    userModels = Stream.of(userModel);
                }
            } else {
                Map<String, String> attributes = new HashMap<>();
                attributes.put(UserModel.SEARCH, search.trim());
                if (enabled != null) {
                    attributes.put(UserModel.ENABLED, enabled.toString());
                }
                session.setAttribute(UserModel.INCLUDE_SERVICE_ACCOUNT, false);
                count = GetUsersQuery.countUsers(session, realm, attributes);
                if (count > 0) {
                    userModels = GetUsersQuery.searchForUserStream(session, realm, attributes, firstResult, maxResults);
                }
            }
        } else if (lastName != null || firstName != null || email != null || username != null || emailVerified != null
                || enabled != null || !searchAttributes.isEmpty()) {
            Map<String, String> attributes = new HashMap<>();
            addWhenNotBlank(attributes, UserModel.LAST_NAME, lastName);
            addWhenNotBlank(attributes, UserModel.FIRST_NAME, firstName);
            addWhenNotBlank(attributes, UserModel.EMAIL, email);
            addWhenNotBlank(attributes, UserModel.USERNAME, username);
            addWhenNotBlank(attributes, UserModel.EMAIL_VERIFIED, emailVerified == null ? null : emailVerified.toString());
            addWhenNotBlank(attributes, UserModel.ENABLED, enabled == null ? null : enabled.toString());

            attributes.putAll(searchAttributes);
            session.setAttribute(UserModel.INCLUDE_SERVICE_ACCOUNT, true);
            count = GetUsersQuery.countUsers(session, realm, attributes);
            if (count > 0) {
                userModels = GetUsersQuery.searchForUserStream(session, realm, attributes, firstResult, maxResults);
            }
        } else {
            session.setAttribute(UserModel.INCLUDE_SERVICE_ACCOUNT, false);
            count = GetUsersQuery.countUsers(session, realm, new HashMap<>());
            if (count > 0) {
                userModels = GetUsersQuery.searchForUserStream(session, realm, new HashMap<>(), firstResult, maxResults);
            }
        }

        return new UsersPageRepresentation(toUserRepresentation(realm, userPermissionEvaluator, briefRepresentation, userModels), count);
    }

    private void addWhenNotBlank(Map<String, String> map, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            map.put(key, value);
        }
    }

    /**
     * Source: keycloak-services/src/main/java/org.keycloak.services.resources.admin.UsersResource
     */
    private List<UserRepresentation> toUserRepresentation(RealmModel realm, UserPermissionEvaluator usersEvaluator, Boolean briefRepresentation, Stream<UserModel> userModels) {
        boolean briefRepresentationB = briefRepresentation != null && briefRepresentation;
        boolean canViewGlobal = usersEvaluator.canView();

        usersEvaluator.grantIfNoPermission(kcSession.getAttribute(UserModel.GROUPS) != null);

        return userModels.map(user -> {
            if (!canViewGlobal && !usersEvaluator.canView(user)) {
                return null;
            }
            UserRepresentation userRep = briefRepresentationB
                    ? ModelToRepresentation.toBriefRepresentation(user)
                    : ModelToRepresentation.toRepresentation(kcSession, realm, user);
            userRep.setAccess(usersEvaluator.getAccess(user));
            return userRep;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
