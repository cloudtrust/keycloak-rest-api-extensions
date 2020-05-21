package io.cloudtrust.keycloak.services.resource.api.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RealmAdminResource extends org.keycloak.services.resources.admin.RealmAdminResource {

    private final AdminEventBuilder adminEvent;

    public RealmAdminResource(AdminPermissionEvaluator auth, RealmModel realm, TokenManager tokenManager,
                              AdminEventBuilder adminEvent, KeycloakSession session) {
        super(auth, realm, tokenManager, adminEvent);
        this.adminEvent = adminEvent;
        this.session = session;
    }

    @Path("users")
    @Override
    public UsersResource users() {
        UsersResource users = new UsersResource(session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(users);
        //resourceContext.initResource(users);
        return users;
    }

    @Path("statistics")
    public StatisticsResource statistics() {
        return new StatisticsResource(session, auth);
    }

    @Path("/locked-users")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<String> getLockedUsers() {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireView();
        userPermissionEvaluator.requireQuery();

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        List<String> result = em.createQuery("select u.id from UserEntity u where u.realmId=:realmId and u.enabled=true")
                .setParameter("realmId", realm.getId())
                .getResultList();
        // UserLoginFailures are stored in InfiniSpan so getUserLoginFailure(...) is supposed to be a fast operation
        long now = System.currentTimeMillis() / 1000;
        return result.stream().filter(userId -> {
            UserLoginFailureModel loginFailure = session.sessions().getUserLoginFailure(realm, userId);
            return loginFailure != null && now < loginFailure.getFailedLoginNotBefore();
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Path("/authenticators-count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Map<String, Long> getAuthenticatorsCount() {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireView();
        userPermissionEvaluator.requireQuery();

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        List<Object[]> result = em.createQuery("select u.id, count(*) from UserEntity u join u.credentials c where u.realmId=:realmId and u.enabled=true group by u.id")
                .setParameter("realmId", realm.getId())
                .getResultList();
        return result.stream().collect(Collectors.toMap(o -> (String) o[0], o -> (Long) o[1]));
    }
}
