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
import java.util.stream.Stream;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public class GetUsersQuery {
    // Constants, searchForUserStream and getSearchOptionPredicateArray are imported from Keycloak code. See JpaUserEntity
    private static final String EMAIL = "email";
    private static final String EMAIL_VERIFIED = "emailVerified";
    private static final String USERNAME = "username";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    /**
     * Imported from Keycloak: count adapt from searchForUserStream
     *
     * @return count of users matching the search criteria
     */
    public static int countUsers(KeycloakSession session, RealmModel realm, Map<String, String> attributes) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> queryBuilder = builder.createQuery(Long.class);
        Root<UserEntity> root = queryBuilder.from(UserEntity.class);
        Expression<Long> count = builder.count(root);

        queryBuilder = queryBuilder.select(count);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realmId"), realm.getId()));

        createPredicates(session, attributes, predicates, root, builder);

        addGroupsPredicate(predicates, session, root, builder, queryBuilder);
        addRolesPredicate(predicates, session, root, builder, queryBuilder);

        queryBuilder.where(predicates.toArray(new Predicate[predicates.size()]));

        TypedQuery<Long> query = em.createQuery(queryBuilder);
        return query.getSingleResult().intValue();
    }

    /**
     * Imported from Keycloak: JpaUserProvider::searchForUserStream
     *
     * @param session     Keycloak session
     * @param realm       Realm we are searching users in
     * @param attributes  Search attributes
     * @param firstResult Pagination: first index
     * @param maxResults  Pagination: max row per page
     * @return Stream of users
     */
    public static Stream<UserModel> searchForUserStream(KeycloakSession session, RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<UserEntity> queryBuilder = builder.createQuery(UserEntity.class);
        Root<UserEntity> root = queryBuilder.from(UserEntity.class);

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realmId"), realm.getId()));

        createPredicates(session, attributes, predicates, root, builder);

        addGroupsPredicate(predicates, session, root, builder, queryBuilder);
        addRolesPredicate(predicates, session, root, builder, queryBuilder);

        queryBuilder.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(builder.asc(root.get(UserModel.USERNAME)));

        TypedQuery<UserEntity> query = em.createQuery(queryBuilder);

        UserProvider users = session.users();
        return closing(paginateQuery(query, firstResult, maxResults).getResultStream())
                .map(userEntity -> users.getUserById(realm, userEntity.getId()));
    }

    private static void createPredicates(KeycloakSession session, Map<String, String> attributes, List<Predicate> predicates, Root<UserEntity> root, CriteriaBuilder builder) {
        Join<Object, Object> federatedIdentitiesJoin = null;
        List<Predicate> attributePredicates = new ArrayList<>();

        if (!session.getAttributeOrDefault(UserModel.INCLUDE_SERVICE_ACCOUNT, true)) {
            predicates.add(root.get("serviceAccountClientLink").isNull());
        }

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
                case USERNAME:
                case EMAIL:
                    if (!"%".equals(value)) {
                        boolean exact = Boolean.valueOf(attributes.getOrDefault(UserModel.EXACT, Boolean.FALSE.toString()));
                        if (value.startsWith("=")) {
                            exact = true;
                            value = value.substring(1);
                        }
                        if (exact) {
                            predicates.add(builder.equal(builder.lower(root.get(key)), value.toLowerCase()));
                        } else if (value.startsWith("%") || value.endsWith("%")) {
                            predicates.add(builder.like(builder.lower(root.get(key)), value.toLowerCase()));
                        } else {
                            predicates.add(builder.like(builder.lower(root.get(key)), "%" + value.toLowerCase() + "%"));
                        }
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

    private static void addGroupsPredicate(List<Predicate> predicates, KeycloakSession session, Root<UserEntity> root, CriteriaBuilder builder, CriteriaQuery<?> queryBuilder) {
        Set<String> userGroups = (Set<String>) session.getAttribute(UserModel.GROUPS);
        if (userGroups != null && !userGroups.isEmpty()) {
            Subquery subquery = queryBuilder.subquery(String.class);
            Root<UserGroupMembershipEntity> from = subquery.from(UserGroupMembershipEntity.class);

            subquery.select(builder.literal(1));

            List<Predicate> subPredicates = new ArrayList<>();

            subPredicates.add(from.get("groupId").in(userGroups));
            subPredicates.add(builder.equal(from.get("user").get("id"), root.get("id")));

            subquery.where(subPredicates.toArray(new Predicate[subPredicates.size()]));

            predicates.add(builder.exists(subquery));
        }
    }

    private static void addRolesPredicate(List<Predicate> predicates, KeycloakSession session, Root<UserEntity> root, CriteriaBuilder builder, CriteriaQuery<?> queryBuilder) {
        Set<String> userRoles = (Set<String>) session.getAttribute("filterRoles");
        if (userRoles != null && !userRoles.isEmpty()) {
            Subquery subquery = queryBuilder.subquery(String.class);
            Root<UserRoleMappingEntity> from = subquery.from(UserRoleMappingEntity.class);

            subquery.select(builder.literal(1));

            List<Predicate> subPredicates = new ArrayList<>();

            subPredicates.add(from.get("roleId").in(userRoles));
            subPredicates.add(builder.equal(from.get("user").get("id"), root.get("id")));

            subquery.where(subPredicates.toArray(new Predicate[subPredicates.size()]));

            predicates.add(builder.exists(subquery));
        }
    }
}
