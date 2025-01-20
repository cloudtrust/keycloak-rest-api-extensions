package io.cloudtrust.keycloak.services.resource.api.account;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.AccountResourceProvider;
import org.keycloak.services.resources.account.CorsPreflightService;
import org.keycloak.theme.Theme;

import java.io.IOException;
import java.util.List;

/**
 * This is copy of org.keycloak.services.resources.account.AccountLoader.
 * It redirects class CorsPreflightService and AccountRestService to our fixed versions.
 */
public class CtAccountLoader {
    private static final Logger logger = Logger.getLogger(CtAccountLoader.class);

    private KeycloakSession session;
    private EventBuilder event;

    public CtAccountLoader(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;
    }

    @Path("/")
    public Object getAccountService() {
        RealmModel realm = session.getContext().getRealm();

        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (client == null || !client.isEnabled()) {
            logger.debug("account management not enabled");
            throw new NotFoundException("account management not enabled");
        }

        HttpRequest request = session.getContext().getHttpRequest();
        HttpHeaders headers = session.getContext().getRequestHeaders();
        MediaType content = headers.getMediaType();
        List<MediaType> accepts = headers.getAcceptableMediaTypes();

        Theme theme = getTheme();
        AccountResourceProvider accountResourceProvider = copiedFromKCGetAccountResourceProvider(theme);

        if (HttpMethod.OPTIONS.equals(request.getHttpMethod())) {
            return new CorsPreflightService();
        } else if ((accepts.contains(MediaType.APPLICATION_JSON_TYPE) || MediaType.APPLICATION_JSON_TYPE.equals(content)) && !session.getContext().getUri().getPath().endsWith("keycloak.json")) {
            AppAuthManager.BearerTokenAuthenticator bearerAuthenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            AuthenticationManager.AuthResult authResult = bearerAuthenticator
                    .setConnection(session.getContext().getConnection())
                    .setHeaders(headers)
                    .authenticate();
            if (authResult == null) {
                throw new NotAuthorizedException("Bearer token required");
            }

            Auth auth = new Auth(session.getContext().getRealm(), authResult.getToken(), authResult.getUser(), client, authResult.getSession(), false);
            return new FixedAccountRestService(session, auth, event);
        } else if (accountResourceProvider != null) {
            return accountResourceProvider.getResource();
        } else {
            throw new NotFoundException();
        }
    }

    /* copied/pasted from Keycloak AccountLoader, to be used by getAccountService() */
    private AccountResourceProvider copiedFromKCGetAccountResourceProvider(Theme theme) {
        try {
            if (theme.getProperties().containsKey(Theme.ACCOUNT_RESOURCE_PROVIDER_KEY)) {
                return session.getProvider(AccountResourceProvider.class, theme.getProperties().getProperty(Theme.ACCOUNT_RESOURCE_PROVIDER_KEY));
            }
        } catch (IOException ignore) {
        }
        return session.getProvider(AccountResourceProvider.class);
    }

    private Theme getTheme() {
        try {
            return session.theme().getTheme(Theme.Type.ACCOUNT);
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
