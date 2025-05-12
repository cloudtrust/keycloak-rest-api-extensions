package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.representations.idm.DeletableUserRepresentation;
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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Encode;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.RealmsAdminResourcePreflight;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CtAdminRoot {
    protected static final Logger logger = Logger.getLogger(CtAdminRoot.class);
    private static final String MSG_AUTH_ADMIN_ACCESS = "authenticated admin access for: {}";

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
     * @param request  HTTP request
     * @param response HTTP response
     * @return administration resource
     */
    @Path("realms")
    public CtRealmsAdminResource getRealmsAdmin() {
        HttpRequest request = session.getContext().getHttpRequest();
        if (HttpMethod.OPTIONS.equals(request.getHttpMethod())) {
            new RealmsAdminResourcePreflight(session, null, tokenManager, request);
        }

        AdminAuth auth = authenticateRealmAdminRequest(request.getHttpHeaders());
        if (auth == null) {
            throw new NotAuthorizedException("Can't get AdminAuth");
        }

        logger.debugf(MSG_AUTH_ADMIN_ACCESS, auth.getUser().getUsername());
        Cors.builder().allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().add();

        return new CtRealmsAdminResource(auth, session);
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
    public List<DeletableUserRepresentation> expiredTermsOfUseAcceptance() {
        logger.warn("Executing expired-tou-acceptance");
        HttpRequest request = this.session.getContext().getHttpRequest();
        AdminAuth auth = authenticateRealmAdminRequest(request.getHttpHeaders());
        if (auth == null) {
            logger.warn("Executing expired-tou-acceptance ** REJECT NO-AUTH **");
            throw new NotAuthorizedException("Can't get AdminAuth");
        }

        logger.debugf(MSG_AUTH_ADMIN_ACCESS, auth.getUser().getUsername());
        Cors.builder().allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().add();

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
     * @param request HTTP request
     * @return
     */
    @Path("support-infos")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<EmailInfo> getSupportInformation(@QueryParam("email") String email) {
        logger.warn("Executing support-infos");
        HttpRequest request = this.session.getContext().getHttpRequest();
        AdminAuth auth = authenticateRealmAdminRequest(request.getHttpHeaders());
        if (auth == null) {
            throw new NotAuthorizedException("unauthorized");
        }

        logger.debugf(MSG_AUTH_ADMIN_ACCESS, auth.getUser().getUsername());
        Cors.builder().allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().add();

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
        return result.stream().map(this::createEmailInfo).toList();
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

    /*
     * Copied/pasted from org.keycloak.services.resources.admin.AdminRoot
     */
    protected AdminAuth authenticateRealmAdminRequest(HttpHeaders headers) {
        String tokenString = AppAuthManager.extractAuthorizationHeaderToken(headers);
        if (tokenString == null) throw new NotAuthorizedException("Bearer");
        AccessToken token;
        try {
            JWSInput input = new JWSInput(tokenString);
            token = input.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            throw new NotAuthorizedException("Bearer token format error");
        }
        String realmName = Encode.decodePath(token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1));
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotAuthorizedException("Unknown realm in token");
        }
        session.getContext().setRealm(realm);

        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(session.getContext().getConnection())
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            logger.debug("Token not valid");
            throw new NotAuthorizedException("Bearer");
        }

        return new AdminAuth(realm, authResult.getToken(), authResult.getUser(), authResult.getClient());
    }
}
