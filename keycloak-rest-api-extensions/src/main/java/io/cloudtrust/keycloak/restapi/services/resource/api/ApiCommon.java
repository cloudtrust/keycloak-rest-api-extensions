package io.cloudtrust.keycloak.restapi.services.resource.api;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import org.keycloak.common.util.Encode;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;

public class ApiCommon {

    /**
     * Key of the session attribute where Keycloak's AdminRoot stores the authenticated AdminAuth (private in AdminRoot);
     * read by AdminRoot#getRealmAdminAuth, used by Keycloak's DefaultPermissions
     */
    static final String ADMIN_AUTH_KEY = "AdminRoot.adminAuth";

    private ApiCommon() {}
    /*
     * Copied/pasted from org.keycloak.services.resources.admin.AdminRoot#authenticateRealmAdminRequest (synced with Keycloak 26.8.1).
     * Our difference: the headers are a parameter (AdminRoot reads them from the session context).
     */
    public static AdminAuth authenticateRealmAdminRequest(KeycloakSession session, HttpHeaders headers) {
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

        session.getContext().setBearerToken(authResult.token());

        AdminAuth result = new AdminAuth(realm, authResult.token(), authResult.user(), authResult.client());
        session.setAttribute(ADMIN_AUTH_KEY, result);
        return result;
    }
}
