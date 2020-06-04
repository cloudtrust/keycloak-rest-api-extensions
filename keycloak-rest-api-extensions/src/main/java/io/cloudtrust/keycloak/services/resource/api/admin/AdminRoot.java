package io.cloudtrust.keycloak.services.resource.api.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminCorsPreflightService;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

public class AdminRoot extends org.keycloak.services.resources.admin.AdminRoot {

    protected static final Logger logger = Logger.getLogger(AdminRoot.class);

    public AdminRoot(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Base Path to realm admin REST interface
     *
     * @param request
     * @return
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

        logger.debug("authenticated admin access for: " + auth.getUser().getUsername());
        Cors.add(request).allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().build(response);

        org.keycloak.services.resources.admin.RealmsAdminResource adminResource = new RealmsAdminResource(auth, tokenManager, session);
        ResteasyProviderFactory.getInstance().injectProperties(adminResource);
        return adminResource;
    }
}
