package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.representations.idm.CredentialsStatisticsRepresentation;
import io.cloudtrust.keycloak.representations.idm.UsersStatisticsRepresentation;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

public class StatisticsResource {
    private static final String[] NOT_AUTHENTICATORS = new String[]{"password", "password-history"};
    private static final String PARAM_REALM = "realmId";

    private AdminPermissionEvaluator auth;
    private KeycloakSession session;
    private RealmModel realm;

    public StatisticsResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.auth = auth;
        this.session = session;
        this.realm = session.getContext().getRealm();
    }

    @Path("users")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UsersStatisticsRepresentation getUsersStatistics() {
        auth.users().requireView();

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        UsersStatisticsRepresentation res = getEnabledUsersStatistics(em);
        long activeUsersCount = getActiveUsersCount(em);
        res.setInactive(res.getTotal() - activeUsersCount);

        return res;
    }

    @SuppressWarnings("unchecked")
    private UsersStatisticsRepresentation getEnabledUsersStatistics(final EntityManager em) {
        long enabledUsersCount = 0;
        long disabledUsersCount = 0;

        List<Object[]> result = em.createQuery("select u.enabled, count(*) from UserEntity u where u.realmId=:realmId group by u.enabled")
                .setParameter(PARAM_REALM, realm.getId()).getResultList();
        for (Object[] row : result) {
            if (Boolean.TRUE.equals(row[0])) {
                enabledUsersCount += (Long) row[1];
            } else {
                disabledUsersCount += (Long) row[1];
            }
        }

        return new UsersStatisticsRepresentation(enabledUsersCount + disabledUsersCount, disabledUsersCount, 0);
    }

    @SuppressWarnings("unchecked")
    private long getActiveUsersCount(EntityManager em) {
        List<Long> result = em.createQuery("select count(*) from UserEntity u join u.credentials c where u.realmId=:realmId and c.type!=:credType1 and c.type!=:credType2 group by u.id")
                .setParameter(PARAM_REALM, realm.getId())
                .setParameter("credType1", NOT_AUTHENTICATORS[0])
                .setParameter("credType2", NOT_AUTHENTICATORS[1])
                .getResultList();
        return result.isEmpty() ? 0 : result.get(0);
    }

    @Path("credentials")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public CredentialsStatisticsRepresentation getCredentialsStatistics() {
        auth.users().requireView();

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CredentialsStatisticsRepresentation asr = new CredentialsStatisticsRepresentation();
        List<Object[]> result = em.createQuery("select c.type, count(*) from UserEntity u join u.credentials c where u.realmId=:realmId group by c.type")
                .setParameter(PARAM_REALM, realm.getId()).getResultList();
        result.forEach(row -> asr.put((String) row[0], (Long) row[1]));

        return asr;
    }
}
