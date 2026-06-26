package io.cloudtrust.keycloak.services.resource.api.idp;

import io.cloudtrust.keycloak.services.resource.api.ApiCommon;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import org.jboss.logging.Logger;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.RealmsAdminResourcePreflight;

public class CtIdpRoot {
    private static final Logger logger = Logger.getLogger(CtIdpRoot.class);

    private final KeycloakSession session;

    public CtIdpRoot(KeycloakSession session) {
        this.session = session;
    }

    @Path("realms")
    public CtIdpRealmsResource getRealms() {
        HttpRequest request = session.getContext().getHttpRequest();
        if (HttpMethod.OPTIONS.equals(request.getHttpMethod())) {
            new RealmsAdminResourcePreflight(session, null, null, request);
        }

        AdminAuth auth = ApiCommon.authenticateRealmAdminRequest(session, request.getHttpHeaders());

        logger.debugf("authenticated idp access for: %s", auth.getUser().getUsername());
        Cors.builder().allowedOrigins(auth.getToken()).allowedMethods("DELETE").exposedHeaders("Location").auth().add();

        return new CtIdpRealmsResource(auth, session);
    }
}
