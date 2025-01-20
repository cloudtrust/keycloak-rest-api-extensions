package io.cloudtrust.keycloak.services.resource.api.account;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.keycloak.http.HttpRequest;
import org.keycloak.services.cors.Cors;

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
        return Cors.builder().auth().allowedMethods("GET", "POST", "HEAD", "OPTIONS").preflight().add(Response.ok());
    }

}
