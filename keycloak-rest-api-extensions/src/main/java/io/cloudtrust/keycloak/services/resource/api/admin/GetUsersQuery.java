package io.cloudtrust.keycloak.services.resource.api.admin;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.models.jpa.entities.UserRoleMappingEntity;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public class GetUsersQuery {
    private static final String SEARCH_ID_PARAMETER = "id:"; // From org.keycloak.services.resources.admin.UsersResource

    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final EntityManager em;
    private CriteriaBuilder builder;
    private CriteriaQuery<UserEntity> userEntityQry;
    private Root<UserEntity> userEntityRoot;
    private List<Predicate> predicates = new ArrayList<>();

    /**
     * Creates an instance of GetUsersQuery: this object is use to build a GetUsers request
     *
     * @param session
     * @param auth
     */
    public GetUsersQuery(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.auth = auth;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        this.builder = em.getCriteriaBuilder();

        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireView();
        userPermissionEvaluator.requireQuery();

        this.userEntityQry = builder.createQuery(UserEntity.class);
        this.userEntityRoot = userEntityQry.from(UserEntity.class);

        predicates.add(builder.equal(userEntityRoot.get("realmId"), session.getContext().getRealm().getId()));
    }

    /**
     * Add predicates to search users using a global criteria which can match part of either the username, the full name (first + last name) or the email
     *
     * @param search The searched value
     */
    public void addPredicateSearchGlobal(String search) {
        if (search.startsWith(SEARCH_ID_PARAMETER)) {
            String id = search.substring(SEARCH_ID_PARAMETER.length()).trim();
            predicates.add(builder.equal(userEntityRoot.get("id"), id));
            return;
        }
        predicates.add(builder.isNull(userEntityRoot.get("serviceAccountClientLink")));

        Expression<String> fullNameExpr = builder.concat(builder.concat(userEntityRoot.get("firstName"), " "), userEntityRoot.get("lastName"));
        predicates.add(builder.or(
                createPredicateLike(fullNameExpr, search),
                createPredicateLike(userEntityRoot.get("username"), search),
                createPredicateLike(userEntityRoot.get("email"), search)
        ));
    }

    /**
     * Add predicates to search users by first name, last name, email and username
     *
     * @param last     Searched last name
     * @param first    Searched first name
     * @param email    Searched email
     * @param username Searched username
     */
    public void addPredicateSearchFields(String last, String first, String email, String username) {
        int count = predicates.size();
        addPredicateLike(userEntityRoot.get(UserModel.LAST_NAME), last);
        addPredicateLike(userEntityRoot.get(UserModel.FIRST_NAME), first);
        addPredicateLike(userEntityRoot.get(UserModel.EMAIL), email);
        addPredicateLike(userEntityRoot.get(UserModel.USERNAME), username);
        boolean includeServiceAccount = predicates.size() != count;
        session.setAttribute(UserModel.INCLUDE_SERVICE_ACCOUNT, includeServiceAccount);
        if (!includeServiceAccount) {
            predicates.add(userEntityRoot.get("serviceAccountClientLink").isNull());
        }
        if (!auth.users().canView()) {
            Set<String> groupModels = auth.groups().getGroupsWithViewPermission();

            if (!groupModels.isEmpty()) {
                session.setAttribute(UserModel.GROUPS, groupModels);
            }
        }
    }

    /**
     * Add a filtering according to the given list of group identifiers
     *
     * @param groups
     */
    public void addPredicateForGroups(List<String> groups) {
        if (groups != null && !groups.isEmpty()) {
            this.auth.groups().requireView();
            predicates.add(builder.exists(createGroupsSubQuery(groups)));
        }
    }

    /**
     * Add a filtering according to the given list of role identifiers
     *
     * @param roles
     */
    public void addPredicateForRoles(List<String> roles) {
        if (roles != null && !roles.isEmpty()) {
            auth.roles().requireView(session.getContext().getRealm());
            predicates.add(userEntityRoot.get("id").in(createRolesSubQuery(roles)));
        }
    }

    private void addPredicateLike(Expression<String> expr, String value) {
        if (value != null && !value.isEmpty()) {
            predicates.add(createPredicateLike(expr, value));
        }
    }

    private Predicate createPredicateLike(Expression<String> expr, String value) {
        if (value.startsWith("=") && value.length() > 1) {
            return builder.equal(builder.lower(expr), value.substring(1));
        }
        String effectiveFilter = value.contains("%") ? value.toLowerCase() : "%" + value.toLowerCase() + "%";
        return builder.like(builder.lower(expr), effectiveFilter);
    }

    private Subquery<?> createGroupsSubQuery(List<String> groups) {
        Subquery<Integer> ugmSubquery = userEntityQry.subquery(Integer.class);
        Root<UserGroupMembershipEntity> from = ugmSubquery.from(UserGroupMembershipEntity.class);

        Predicate[] subPredicates = new Predicate[]{
                from.get("groupId").in(groups),
                builder.equal(from.get("user").get("id"), userEntityRoot.get("id"))
        };

        return ugmSubquery
                .select(this.builder.literal(1))
                .where(subPredicates);
    }

    private Subquery<?> createRolesSubQuery(List<String> roles) {
        Subquery<UserRoleMappingEntity> urmSubquery = userEntityQry.subquery(UserRoleMappingEntity.class);
        Root<UserRoleMappingEntity> userRoleMembership = urmSubquery.from(UserRoleMappingEntity.class);
        return urmSubquery
                .select(userRoleMembership.get("user").get("id"))
                .where(userRoleMembership.get("roleId").in(roles));
    }

    /**
     * Predicates have been created: use them in the entity query
     */
    public void applyPredicates() {
        userEntityQry.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(builder.asc(userEntityRoot.get(UserModel.USERNAME)));
        predicates.clear();
    }

    /**
     * Get the total number of matched rows
     *
     * @return number of matched rows
     */
    public int getTotalCount() {
        return em.createQuery(userEntityQry).getResultList().size();
    }

    /**
     * Get a view of the matched rows
     *
     * @param firstResult
     * @param maxResults
     * @return
     */
    public Stream<UserModel> execute(Integer firstResult, Integer maxResults) {
        TypedQuery<UserEntity> query = em.createQuery(userEntityQry);

        RealmModel realm = session.getContext().getRealm();
        UserProvider users = session.users();

        return closing(paginateQuery(query, firstResult, maxResults).getResultStream())
                .map(entity -> users.getUserById(realm, entity.getId()));
    }

    // Constants, searchForUserStream and getSearchOptionPredicateArray are imported from Keycloak code. See JpaUserEntity
    private static final String EMAIL = "email";
    private static final String EMAIL_VERIFIED = "emailVerified";
    private static final String USERNAME = "username";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    public static Stream<UserModel> searchForUserStream(KeycloakSession session, RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<UserEntity> queryBuilder = builder.createQuery(UserEntity.class);
        Root<UserEntity> root = queryBuilder.from(UserEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        List<Predicate> attributePredicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realmId"), realm.getId()));

        if (!session.getAttributeOrDefault(UserModel.INCLUDE_SERVICE_ACCOUNT, true)) {
            predicates.add(root.get("serviceAccountClientLink").isNull());
        }

        Join<Object, Object> federatedIdentitiesJoin = null;

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null) {
                continue;
            }

            switch (key) {
                case UserModel.SEARCH:
                    for (String stringToSearch : value.trim().split("\\s+")) {
                        predicates.add(builder.or(getSearchOptionPredicateArray(stringToSearch, builder, root)));
                    }
                    break;
                case FIRST_NAME:
                case LAST_NAME:
                    if (Boolean.valueOf(attributes.getOrDefault(UserModel.EXACT, Boolean.FALSE.toString()))) {
                        predicates.add(builder.equal(builder.lower(root.get(key)), value.toLowerCase()));
                    } else {
                        predicates.add(builder.like(builder.lower(root.get(key)), "%" + value.toLowerCase() + "%"));
                    }
                    break;
                case USERNAME:
                case EMAIL:
                    if (Boolean.valueOf(attributes.getOrDefault(UserModel.EXACT, Boolean.FALSE.toString()))) {
                        predicates.add(builder.equal(root.get(key), value.toLowerCase()));
                    } else {
                        predicates.add(builder.like(root.get(key), "%" + value.toLowerCase() + "%"));
                    }
                    break;
                case EMAIL_VERIFIED:
                    predicates.add(builder.equal(root.get(key), Boolean.parseBoolean(value.toLowerCase())));
                    break;
                case UserModel.ENABLED:
                    predicates.add(builder.equal(root.get(key), Boolean.parseBoolean(value)));
                    break;
                case UserModel.IDP_ALIAS:
                    if (federatedIdentitiesJoin == null) {
                        federatedIdentitiesJoin = root.join("federatedIdentities");
                    }
                    predicates.add(builder.equal(federatedIdentitiesJoin.get("identityProvider"), value));
                    break;
                case UserModel.IDP_USER_ID:
                    if (federatedIdentitiesJoin == null) {
                        federatedIdentitiesJoin = root.join("federatedIdentities");
                    }
                    predicates.add(builder.equal(federatedIdentitiesJoin.get("userId"), value));
                    break;
                case UserModel.EXACT:
                    break;
                // All unknown attributes will be assumed as custom attributes
                default:
                    Join<UserEntity, UserAttributeEntity> attributesJoin = root.join("attributes", JoinType.LEFT);

                    attributePredicates.add(builder.and(
                            builder.equal(builder.lower(attributesJoin.get("name")), key.toLowerCase()),
                            builder.equal(builder.lower(attributesJoin.get("value")), value.toLowerCase())));

                    break;
            }
        }

        if (!attributePredicates.isEmpty()) {
            predicates.add(builder.and(attributePredicates.toArray(new Predicate[0])));
        }

        Set<String> userGroups = (Set<String>) session.getAttribute(UserModel.GROUPS);

        if (userGroups != null) {
            Subquery subquery = queryBuilder.subquery(String.class);
            Root<UserGroupMembershipEntity> from = subquery.from(UserGroupMembershipEntity.class);

            subquery.select(builder.literal(1));

            List<Predicate> subPredicates = new ArrayList<>();

            subPredicates.add(from.get("groupId").in(userGroups));
            subPredicates.add(builder.equal(from.get("user").get("id"), root.get("id")));

            subquery.where(subPredicates.toArray(new Predicate[subPredicates.size()]));

            predicates.add(builder.exists(subquery));
        }

        queryBuilder.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(builder.asc(root.get(UserModel.USERNAME)));

        TypedQuery<UserEntity> query = em.createQuery(queryBuilder);

        UserProvider users = session.users();
        return closing(paginateQuery(query, firstResult, maxResults).getResultStream())
                .map(userEntity -> users.getUserById(realm, userEntity.getId()));
    }

    private static Predicate[] getSearchOptionPredicateArray(String value, CriteriaBuilder builder, From<?, UserEntity> from) {
        value = value.toLowerCase();

        List<Predicate> orPredicates = new ArrayList<>();

        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            // exact search
            value = value.substring(1, value.length() - 1);

            orPredicates.add(builder.equal(from.get(USERNAME), value));
            orPredicates.add(builder.equal(from.get(EMAIL), value));
            orPredicates.add(builder.equal(builder.lower(from.get(FIRST_NAME)), value));
            orPredicates.add(builder.equal(builder.lower(from.get(LAST_NAME)), value));
        } else {
            if (value.length() >= 2 && value.charAt(0) == '*' && value.charAt(value.length() - 1) == '*') {
                // infix search
                value = "%" + value.substring(1, value.length() - 1) + "%";
            } else {
                // default to prefix search
                if (value.length() > 0 && value.charAt(value.length() - 1) == '*') {
                    value = value.substring(0, value.length() - 1);
                }
                value += "%";
            }

            orPredicates.add(builder.like(from.get(USERNAME), value));
            orPredicates.add(builder.like(from.get(EMAIL), value));
            orPredicates.add(builder.like(builder.lower(from.get(FIRST_NAME)), value));
            orPredicates.add(builder.like(builder.lower(from.get(LAST_NAME)), value));
        }

        return orPredicates.toArray(new Predicate[0]);
    }
}
