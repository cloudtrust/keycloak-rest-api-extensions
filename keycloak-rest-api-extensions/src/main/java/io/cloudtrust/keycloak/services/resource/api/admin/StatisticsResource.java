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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
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

    private static final String QUERY_ACTIVE_USERS_COUNT =
            "select count(distinct u.ID) "
                    + "from USER_ENTITY u "
                    + "join CREDENTIAL c on u.ID=c.USER_ID and c.TYPE!=:credType1 and c.TYPE!=:credType2 "
                    + "where u.REALM_ID=:realmId";

    @SuppressWarnings("unchecked")
    private long getActiveUsersCount(EntityManager em) {
        List<BigInteger> result = em.createNativeQuery(QUERY_ACTIVE_USERS_COUNT)
                .setParameter(PARAM_REALM, realm.getId())
                .setParameter("credType1", NOT_AUTHENTICATORS[0])
                .setParameter("credType2", NOT_AUTHENTICATORS[1])
                .getResultList();
        return result.isEmpty() ? 0 : result.get(0).longValue();
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

    private static final String QUERY_ONBOARDING_STATISTICS =
            "select sum(stats.shadow) shadow, sum(stats.email_verified) email_verified, sum(stats.phone_verified) phone_verified," +
                    "       sum(stats.otp) otp, sum(stats.shadow)-sum(stats.inactivated) activated " +
                    "from (select 1 shadow," +
                    "        CASE WHEN utrust.EMAIL_VERIFIED is not null THEN 1 ELSE 0 END email_verified," +
                    "        CASE WHEN uphone.VALUE is not null THEN 1 ELSE 0 END phone_verified," +
                    "        CASE WHEN cotp.ID is not null THEN 1 ELSE 0 END otp," +
                    "        CASE WHEN cact.ID is not null THEN 1 ELSE 0 END inactivated " +
                    "from USER_ENTITY ushad " +
                    "left outer join USER_ENTITY utrust ON utrust.USERNAME=ushad.USERNAME AND utrust.REALM_ID=:socialRealmId " +
                    "left outer join USER_ATTRIBUTE uphone ON uphone.USER_ID=utrust.ID AND uphone.NAME='phoneNumberVerified' AND uphone.VALUE='true' " +
                    "left outer join CREDENTIAL cotp ON cotp.USER_ID=utrust.ID AND cotp.TYPE='ctotp' " +
                    "left outer join CREDENTIAL cact ON cact.USER_ID=ushad.ID AND cact.TYPE='activationcode' " +
                    "where ushad.REALM_ID=:clientRealmId " +
                    "  and ushad.CREATED_TIMESTAMP BETWEEN :dateFrom AND :dateTo " +
                    "group by ushad.ID, utrust.EMAIL_VERIFIED, uphone.VALUE, cotp.ID, cact.ID) stats";
    private static final String[] ONBOARDING_STATISTICS_RESULTS = new String[]{
            "invitedUsers", "verifiedEmails", "verifiedPhones", "secondFactors", "activatedAccounts"
    };

    @Path("onboarding")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public CredentialsStatisticsRepresentation getOnboardingStatistics(@QueryParam("socialRealmId") String socialRealmId, @QueryParam("dateFrom") Long dateFrom, @QueryParam("dateTo") Long dateTo) {
        auth.users().requireView();

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CredentialsStatisticsRepresentation asr = new CredentialsStatisticsRepresentation();
        em.createNativeQuery(QUERY_ONBOARDING_STATISTICS)
                .setParameter("socialRealmId", socialRealmId)
                .setParameter("clientRealmId", realm.getId())
                .setParameter("dateFrom", dateFrom)
                .setParameter("dateTo", dateTo)
                .getResultStream()
                .forEach(res -> {
                    Object[] values = (Object[]) res;
                    for (int idx = 0; idx < ONBOARDING_STATISTICS_RESULTS.length; idx++) {
                        asr.put(ONBOARDING_STATISTICS_RESULTS[idx], ((BigInteger) values[idx]).longValue());
                    }
                });

        return asr;
    }
}
