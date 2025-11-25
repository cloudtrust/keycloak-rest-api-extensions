package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.representations.idm.UsersPageRepresentation;
import io.quarkus.logging.Log;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.common.Profile;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.policy.PasswordPolicyNotMetException;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.utils.SearchQueryUtils;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static org.keycloak.models.utils.KeycloakModelUtils.findGroupByPath;
import static org.keycloak.userprofile.UserProfileContext.USER_API;

public class CtUsersResource {
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final KeycloakSession session;

    public CtUsersResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.auth = auth;
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
            if (!inheritedCanCreateGroupMembers(rep)) {
                throw exception;
            }
        }

        RealmModel realm = this.session.getContext().getRealm();
        String username = rep.getUsername();
        if (realm.isRegistrationEmailAsUsername()) {
            username = rep.getEmail();
        }

        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);

        UserProfile profile = profileProvider.create(USER_API, rep.getRawAttributes());

        try {
            Response response = UserResource.validateUserProfile(profile, session, auth.adminAuth());
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

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(user.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("User exists with same username or email");
        } catch (PasswordPolicyNotMetException e) {
            Log.warn("Password policy not met for user " + e.getUsername(), e);
            Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale());
            throw new ErrorResponseException(e.getMessage(), MessageFormat.format(messages.getProperty(e.getMessage(), e.getMessage()), e.getParameters()),
                    Response.Status.BAD_REQUEST);
        } catch (ModelIllegalStateException e) {
            Log.error(e.getMessage(), e);
            throw ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        } catch (ModelException me){
            Log.warn("Could not create user", me);
            throw ErrorResponse.error("Could not create user", Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Copied/pasted from inherited UsersResource
     */
    private boolean inheritedCanCreateGroupMembers(UserRepresentation rep) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
            return false;
        }

        RealmModel realm = this.session.getContext().getRealm();
        List<GroupModel> groups = Optional.ofNullable(rep.getGroups())
                .orElse(Collections.emptyList())
                .stream().map(path -> findGroupByPath(session, realm, path))
                .filter(Objects::nonNull)
                .toList();

        if (groups.isEmpty()) {
            return false;
        }

        // if groups is part of the user rep, check if admin has manage_members and manage_membership on each group
        // an exception is thrown in case the current user does not have permissions to manage any of the groups
        for (GroupModel group : groups) {
            auth.groups().requireManageMembers(group);
            auth.groups().requireManageMembership(group);
        }

        return true;
    }

    private List<GroupModel> getGroups(List<String> groups) {
        List<GroupModel> res = new ArrayList<>();
        if (groups != null) {
            RealmModel realm = this.session.getContext().getRealm();
            for (String groupId : groups) {
                GroupModel group = this.session.realms().getRealm(realm.getId()).getGroupsStream().filter(g -> groupId.equals(g.getId())).findFirst().orElse(null);
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
            RealmModel realm = this.session.getContext().getRealm();
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
    @Path("{user-id}")
    public CtUserResource user(final @PathParam("user-id") String id) {
        RealmModel realm = this.session.getContext().getRealm();
        UserModel user = this.session.users().getUserById(realm, id);
        if (user == null) {
            // we do this to make sure nobody can phish ids
            if (auth.users().canQuery()) throw new NotFoundException("User not found");
            else throw new ForbiddenException();
        }
        return new CtUserResource(this.session, user, auth, adminEvent);
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
     * Returns a stream of users, filtered according to query parameters.
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
     * @param idpAlias            The alias of an Identity Provider linked to the user
     * @param idpUserId           The userId at an Identity Provider linked to the user
     * @param firstResult         Pagination offset
     * @param maxResults          Maximum results size (defaults to 100) - only taken into account if no group / role is defined
     * @param briefRepresentation Should the API return a brief representation or the standard one
     * @param exact               Boolean which defines whether the params "last", "first", "email" and "username" must match exactly
     * @param searchQuery         A query to search for custom attributes, in the format 'key1:value2 key2:value2'
     * @return a non-null {@code Stream} of users
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UsersPageRepresentation getUsers(@QueryParam("groupId") List<String> groups,
                                            @QueryParam("roleId") List<String> roles,
                                            @Parameter(description = "A String contained in username, first or last name, or email. Default search behavior is prefix-based (e.g., foo or foo*). Use *foo* for infix search and \"foo\" for exact search.") @QueryParam("search") String search,
                                            @Parameter(description = "A String contained in lastName, or the complete lastName, if param \"exact\" is true") @QueryParam("lastName") String last,
                                            @Parameter(description = "A String contained in firstName, or the complete firstName, if param \"exact\" is true") @QueryParam("firstName") String first,
                                            @Parameter(description = "A String contained in email, or the complete email, if param \"exact\" is true") @QueryParam("email") String email,
                                            @Parameter(description = "A String contained in username, or the complete username, if param \"exact\" is true") @QueryParam("username") String username,
                                            @Parameter(description = "whether the email has been verified") @QueryParam("emailVerified") Boolean emailVerified,
                                            @Parameter(description = "The alias of an Identity Provider linked to the user") @QueryParam("idpAlias") String idpAlias,
                                            @Parameter(description = "The userId at an Identity Provider linked to the user") @QueryParam("idpUserId") String idpUserId,
                                            @Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
                                            @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults,
                                            @Parameter(description = "Boolean representing if user is enabled or not") @QueryParam("enabled") Boolean enabled,
                                            @Parameter(description = "Boolean which defines whether brief representations are returned (default: false)") @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                            @Parameter(description = "Boolean which defines whether the params \"last\", \"first\", \"email\" and \"username\" must match exactly") @QueryParam("exact") Boolean exact,
                                            @Parameter(description = "A query to search for custom attributes, in the format 'key1:value2 key2:value2'") @QueryParam("q") String searchQuery) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireQuery();

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        Map<String, String> searchAttributes = searchQuery == null
                ? Collections.emptyMap()
                : SearchQueryUtils.getFields(searchQuery);

        if (!CollectionUtil.isEmpty(groups)) {
            session.setAttribute(UserModel.GROUPS, new HashSet<>(groups));
        }
        if (!CollectionUtil.isEmpty(roles)) {
            session.setAttribute("filterRoles", new HashSet<>(roles));
        }

        RealmModel realm = this.session.getContext().getRealm();
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
        } else if (last != null || first != null || email != null || username != null || emailVerified != null
                || idpAlias != null || idpUserId != null || enabled != null || exact != null || !searchAttributes.isEmpty()) {
            Map<String, String> attributes = new HashMap<>();
            addWhenNotBlank(attributes, UserModel.LAST_NAME, last);
            addWhenNotBlank(attributes, UserModel.FIRST_NAME, first);
            addWhenNotBlank(attributes, UserModel.EMAIL, email);
            addWhenNotBlank(attributes, UserModel.USERNAME, username);
            addWhenNotBlank(attributes, UserModel.EMAIL_VERIFIED, emailVerified == null ? null : emailVerified.toString());
            addWhenNotBlank(attributes, UserModel.IDP_ALIAS, idpAlias);
            addWhenNotBlank(attributes, UserModel.IDP_USER_ID, idpUserId);
            addWhenNotBlank(attributes, UserModel.ENABLED, enabled == null ? null : enabled.toString());
            addWhenNotBlank(attributes, UserModel.EXACT, exact == null ? null : exact.toString());

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

        usersEvaluator.grantIfNoPermission(this.session.getAttribute(UserModel.GROUPS) != null);

        return userModels
                .filter(user -> canViewGlobal || usersEvaluator.canView(user))
                .map(user -> {
                    UserRepresentation userRep = briefRepresentationB
                        ? ModelToRepresentation.toBriefRepresentation(user)
                        : ModelToRepresentation.toRepresentation(this.session, realm, user);
                    userRep.setAccess(usersEvaluator.getAccess(user));
                    return userRep;
                })
                .toList();
    }
}
