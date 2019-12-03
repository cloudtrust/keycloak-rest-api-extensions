package io.cloudtrust.keycloak.services.resource.api.account;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * This is a copy of org.keycloak.services.resources.account.CorsPreflightService that fixes a bug with CORS
 * The inspiration for the fix came from org.keycloak.services.resources.admin.AdminCorsPreflightService.
 */
public class FixedCorsPreflightService {

    private HttpRequest request;

    public FixedCorsPreflightService(HttpRequest request) {
        this.request = request;
    }

    /**
     * CORS preflight
     *
     * @return
     */
    @Path("{any:.*}")
    @OPTIONS
    public Response preflight() {
        Cors cors = Cors.add(request, Response.ok()).auth().allowedMethods("GET", "POST", "HEAD", "OPTIONS").preflight();
        return cors.build();
    }

}
