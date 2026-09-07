package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.representations.idm.DeletableUserRepresentation;
import io.cloudtrust.keycloak.services.resource.JpaResultCaster;
import io.cloudtrust.keycloak.services.resource.api.ApiCommon;
import io.cloudtrust.keycloak.services.resource.api.ApiConfig;
import io.cloudtrust.keycloak.services.resource.api.model.EmailInfo;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.RealmsAdminResourcePreflight;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CtAdminRoot {
    protected static final Logger logger = Logger.getLogger(CtAdminRoot.class);
    private static final String MSG_AUTH_ADMIN_ACCESS = "authenticated admin access for: {}";
    private static final String LOCATION = "Location";
    private static final String DELETE = "DELETE";

    private final KeycloakSession session;
    private final ApiConfig apiConfig;
    private final TokenManager tokenManager;

    public CtAdminRoot(KeycloakSession session, ApiConfig apiConfig) {
        this.session = session;
        this.apiConfig = apiConfig;
        this.tokenManager = new TokenManager();
    }

    /**
     * Base Path to realm admin REST interface
     *
     * @return administration resource
     */
    @Path("realms")
    public CtRealmsAdminResource getRealmsAdmin() {
        HttpRequest request = session.getContext().getHttpRequest();
        if (HttpMethod.OPTIONS.equals(request.getHttpMethod())) {
            new RealmsAdminResourcePreflight(session, null, tokenManager, request);
        }

        AdminAuth auth = ApiCommon.authenticateRealmAdminRequest(this.session, request.getHttpHeaders());
        if (auth == null) {
            throw new NotAuthorizedException("Can't get AdminAuth");
        }

        logger.debugf(MSG_AUTH_ADMIN_ACCESS, auth.getUser().getUsername());

        Cors.builder().checkAllowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", DELETE).exposedHeaders(LOCATION).auth().add();

        return new CtRealmsAdminResource(auth, session);
    }

    /**
     * Get the list of users who did not accept terms of use in the given delay
     *
     * @return
     */
    @Path("expired-tou-acceptance")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeletableUserRepresentation> expiredTermsOfUseAcceptance() {
        logger.warn("Executing expired-tou-acceptance");
        HttpRequest request = this.session.getContext().getHttpRequest();
        AdminAuth auth = ApiCommon.authenticateRealmAdminRequest(session, request.getHttpHeaders());
        if (auth == null) {
            logger.warn("Executing expired-tou-acceptance ** REJECT NO-AUTH **");
            throw new NotAuthorizedException("Can't get AdminAuth");
        }

        logger.debugf(MSG_AUTH_ADMIN_ACCESS, auth.getUser().getUsername());
        Cors.builder().checkAllowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", DELETE).exposedHeaders(LOCATION).auth().add();

        // Check rights
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getKeycloakAdministrationRealm();
        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);
        realmAuth.users().requireManage();

        long limit = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(apiConfig.getTermsOfUseAcceptanceDelayMillis());
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        @SuppressWarnings("unchecked")
        List<Object[]> result = em.createNativeQuery("select u.ID, u.USERNAME, r.ID REALM_ID, r.NAME REALM_NAME "
                        + "from USER_ENTITY u "
                        + "inner join USER_REQUIRED_ACTION ura ON ura.USER_ID=u.ID AND ura.REQUIRED_ACTION=:requiredAction "
                        + "inner join REALM r ON r.ID=u.REALM_ID " + "where u.CREATED_TIMESTAMP<:limit")
                .setParameter("requiredAction", "ct-terms-of-use")
                .setParameter("limit", limit)
                .getResultList();
        logger.debugf("expiredTermsOfUseAcceptance> found %d rows", result.size());
        return result.stream().map(this::createDeletableUser).toList();
    }

    private DeletableUserRepresentation createDeletableUser(Object[] userInfo) {
        DeletableUserRepresentation res = new DeletableUserRepresentation();
        res.setUserId((String) userInfo[0]);
        res.setUsername((String) userInfo[1]);
        res.setRealmId((String) userInfo[2]);
        res.setRealmName((String) userInfo[3]);
        return res;
    }

    /**
     * Get information relative to a given email address
     *
     * @param email email address
     * @return
     */
    @Path("support-infos")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<EmailInfo> getSupportInformation(@QueryParam("email") String email) {
        logger.warn("Executing support-infos");
        HttpRequest request = this.session.getContext().getHttpRequest();
        AdminAuth auth = ApiCommon.authenticateRealmAdminRequest(session, request.getHttpHeaders());
        if (auth == null) {
            throw new NotAuthorizedException("unauthorized");
        }

        logger.debugf(MSG_AUTH_ADMIN_ACCESS, auth.getUser().getUsername());
        Cors.builder().checkAllowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", DELETE).exposedHeaders(LOCATION).auth().add();

        // Check rights
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getKeycloakAdministrationRealm();
        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);
        realmAuth.users().requireView();

        if (StringUtils.isBlank(email)) {
            throw new BadRequestException("email");
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        @SuppressWarnings("unchecked")
        List<Object[]> result = em.createNativeQuery("select r.NAME, ue.CREATED_TIMESTAMP "
                        + "from USER_ENTITY ue "
                        + "inner join REALM r ON r.ID=ue.REALM_ID "
                        + "where lower(ue.EMAIL)=lower(:email)")
                .setParameter("email", email)
                .getResultList();
        if (result.isEmpty()) {
            throw new NotFoundException("email");
        }
        return result.stream().map(this::createEmailInfo).toList();
    }

    private EmailInfo createEmailInfo(Object[] row) {
        EmailInfo res = new EmailInfo();
        res.setRealm(JpaResultCaster.toString(row[0]));
        res.setCreationDate(JpaResultCaster.toLong(row[1]));
        return res;
    }
}
