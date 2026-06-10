package io.cloudtrust.keycloak.services.resource.api.idp;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Encode;
import org.keycloak.http.HttpRequest;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
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

        AdminAuth auth = authenticateRealmAdminRequest(request.getHttpHeaders());

        logger.debugf("authenticated idp access for: %s", auth.getUser().getUsername());
        Cors.builder().allowedOrigins(auth.getToken()).allowedMethods("DELETE").exposedHeaders("Location").auth().add();

        return new CtIdpRealmsResource(auth, session);
    }

    private AdminAuth authenticateRealmAdminRequest(HttpHeaders headers) {
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
            throw new NotAuthorizedException("Bearer");
        }

        return new AdminAuth(realm, authResult.getToken(), authResult.getUser(), authResult.getClient());
    }
}
