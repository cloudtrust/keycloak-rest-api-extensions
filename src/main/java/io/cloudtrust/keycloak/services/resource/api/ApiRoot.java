package io.cloudtrust.keycloak.services.resource.api;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminCorsPreflightService;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.RealmsAdminResource;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

public class ApiRoot extends AdminRoot {

    protected static final Logger logger = Logger.getLogger(ApiRoot.class);

    public ApiRoot(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Base Path to realm admin REST interface
     *
     * @param request
     * @return
     */
    @Path("realms")
    public Object getRealmsAdmin(@Context final HttpRequest request) {
        if (request.getHttpMethod().equals(HttpMethod.OPTIONS)) {
            return new AdminCorsPreflightService(request);
        }

        AdminAuth auth = authenticateRealmAdminRequest(request.getHttpHeaders());
        if (auth != null) {
            logger.debug("authenticated admin access for: " + auth.getUser().getUsername());
        }

        Cors.add(request).allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().build(response);

        RealmsAdminResource adminResource = new RealmsApiResource(auth, tokenManager, session);
        ResteasyProviderFactory.getInstance().injectProperties(adminResource);
        return adminResource;
    }
}
