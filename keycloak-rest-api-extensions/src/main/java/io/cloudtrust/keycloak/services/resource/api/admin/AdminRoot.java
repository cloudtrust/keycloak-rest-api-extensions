package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.representations.idm.DeletableUserRepresentation;
import io.cloudtrust.keycloak.services.resource.api.ApiConfig;
import io.cloudtrust.keycloak.services.resource.api.model.EmailInfo;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminCorsPreflightService;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import javax.persistence.EntityManager;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AdminRoot extends org.keycloak.services.resources.admin.AdminRoot {

    protected static final Logger logger = Logger.getLogger(AdminRoot.class);
    private static final String MSG_AUTH_ADMIN_ACCESS = "authenticated admin access for: {}";

    private final ApiConfig apiConfig;

    public AdminRoot(KeycloakSession session, ApiConfig apiConfig) {
        this.session = session;
        this.apiConfig = apiConfig;
    }

    /**
     * Base Path to realm admin REST interface
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @return administration resource
     */
    @Path("realms")
    public Object getRealmsAdmin(@Context final HttpRequest request, @Context HttpResponse response) {
        if (HttpMethod.OPTIONS.equals(request.getHttpMethod())) {
            return new AdminCorsPreflightService(request);
        }

        AdminAuth auth = authenticateRealmAdminRequest(request.getHttpHeaders());
        if (auth == null) {
            throw new NotAuthorizedException("Can't get AdminAuth");
        }

        logger.debugf(MSG_AUTH_ADMIN_ACCESS, auth.getUser().getUsername());
        Cors.add(request).allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().build(response);

        org.keycloak.services.resources.admin.RealmsAdminResource adminResource = new RealmsAdminResource(auth, tokenManager, session);
        ResteasyProviderFactory.getInstance().injectProperties(adminResource);
        return adminResource;
    }

    /**
     * Get the list of users who did not accept terms of use in the given delay
     *
     * @param request HTTP request
     * @return
     */
    @Path("expired-tou-acceptance")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeletableUserRepresentation> expiredTermsOfUseAcceptance(@Context final HttpRequest request, @Context HttpResponse response) {
        AdminAuth auth = authenticateRealmAdminRequest(request.getHttpHeaders());
        if (auth == null) {
            throw new NotAuthorizedException("Can't get AdminAuth");
        }

        logger.debugf(MSG_AUTH_ADMIN_ACCESS, auth.getUser().getUsername());
        Cors.add(request).allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().build(response);

        // Check rights
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getKeycloakAdminstrationRealm();
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
        return result.stream().map(this::createDeletableUser).collect(Collectors.toList());
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
     * @param request HTTP request
     * @return
     */
    @Path("support-infos")
    @GET
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public List<EmailInfo> getSupportInformation(@Context final HttpRequest request, @Context HttpResponse response, @QueryParam("email") String email) {
        AdminAuth auth = authenticateRealmAdminRequest(request.getHttpHeaders());
        if (auth == null) {
            throw new NotAuthorizedException("unauthorized");
        }

        logger.debugf(MSG_AUTH_ADMIN_ACCESS, auth.getUser().getUsername());
        Cors.add(request).allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().build(response);

        // Check rights
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getKeycloakAdminstrationRealm();
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
        return result.stream().map(this::createEmailInfo).collect(Collectors.toList());
    }

    private EmailInfo createEmailInfo(Object[] row) {
        EmailInfo res = new EmailInfo();
        res.setRealm((String) row[0]);
        BigInteger creationDate = (BigInteger) row[1];
        if (creationDate != null) {
            res.setCreationDate(creationDate.longValue());
        }
        return res;
    }
}
