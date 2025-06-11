package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.representations.idm.CredentialsStatisticsRepresentation;
import io.cloudtrust.keycloak.representations.idm.UsersStatisticsRepresentation;
import io.cloudtrust.keycloak.services.resource.JpaResultCaster;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.util.List;

public class StatisticsResource {
    private static final String[] NOT_AUTHENTICATORS = new String[]{PasswordCredentialModel.TYPE, PasswordCredentialModel.PASSWORD_HISTORY};
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
                enabledUsersCount += JpaResultCaster.toLong(row[1]);
            } else {
                disabledUsersCount += JpaResultCaster.toLong(row[1]);
            }
        }

        return new UsersStatisticsRepresentation(enabledUsersCount + disabledUsersCount, disabledUsersCount, 0);
    }

    private static final String QUERY_ACTIVE_USERS_COUNT =
            "select count(distinct u.ID) "
                    + "from USER_ENTITY u "
                    + "join CREDENTIAL c on u.ID=c.USER_ID and c.TYPE!=:credType1 and c.TYPE!=:credType2 "
                    + "where u.REALM_ID=:realmId";

    @SuppressWarnings("unchecked")
    private long getActiveUsersCount(EntityManager em) {
        List<Long> result = em.createNativeQuery(QUERY_ACTIVE_USERS_COUNT)
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
        result.forEach(row -> asr.put(JpaResultCaster.toString(row[0]), JpaResultCaster.toLong(row[1])));

        return asr;
    }
}
