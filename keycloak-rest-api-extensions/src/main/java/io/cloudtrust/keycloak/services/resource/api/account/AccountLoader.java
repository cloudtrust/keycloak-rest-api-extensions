package io.cloudtrust.keycloak.services.resource.api.account;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.account.AccountConsole;
import org.keycloak.services.resources.account.AccountFormService;
import org.keycloak.theme.Theme;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

/**
 * This is copy of org.keycloak.services.resources.account.AccountLoader.
 * It redirects class CorsPreflightService and AccountRestService to our fixed versions.
 */
public class AccountLoader extends org.keycloak.services.resources.account.AccountLoader {

    private static final Logger logger = Logger.getLogger(org.keycloak.services.resources.account.AccountLoader.class);

    @Override
    public Object getAccountService(KeycloakSession session, EventBuilder event) {
        RealmModel realm = session.getContext().getRealm();

        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (client == null || !client.isEnabled()) {
            logger.debug("account management not enabled");
            throw new NotFoundException("account management not enabled");
        }

        HttpRequest request = session.getContext().getContextObject(HttpRequest.class);
        HttpHeaders headers = session.getContext().getRequestHeaders();
        MediaType content = headers.getMediaType();
        List<MediaType> accepts = headers.getAcceptableMediaTypes();

        Theme theme = getTheme(session);
        boolean deprecatedAccount = isDeprecatedFormsAccountConsole(theme);

        if (HttpMethod.OPTIONS.equals(request.getHttpMethod())) {
            return new FixedCorsPreflightService(request);
        } else if ((accepts.contains(MediaType.APPLICATION_JSON_TYPE) || MediaType.APPLICATION_JSON_TYPE.equals(content)) && !request.getUri().getPath().endsWith("keycloak.json")) {
            AuthenticationManager.AuthResult authResult = new AppAuthManager().authenticateBearerToken(session);
            if (authResult == null) {
                throw new NotAuthorizedException("Bearer token required");
            }

            Auth auth = new Auth(session.getContext().getRealm(), authResult.getToken(), authResult.getUser(), client, authResult.getSession(), false);
            FixedAccountRestService accountRestService = new FixedAccountRestService(session, auth, client, event);
            ResteasyProviderFactory.getInstance().injectProperties(accountRestService);
            accountRestService.init();
            return accountRestService;
        } else {
            if (deprecatedAccount) {
                AccountFormService accountFormService = new AccountFormService(realm, client, event);
                ResteasyProviderFactory.getInstance().injectProperties(accountFormService);
                accountFormService.init();
                return accountFormService;
            } else {
                AccountConsole console = new AccountConsole(realm, client, theme);
                ResteasyProviderFactory.getInstance().injectProperties(console);
                console.init();
                return console;
            }
        }
    }

    private Theme getTheme(KeycloakSession session) {
        try {
            return session.theme().getTheme(Theme.Type.ACCOUNT);
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private boolean isDeprecatedFormsAccountConsole(Theme theme) {
        try {
            return Boolean.parseBoolean(theme.getProperties().getProperty("deprecatedMode", "true"));
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
