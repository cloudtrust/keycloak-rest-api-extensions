package io.cloudtrust.keycloak.config;

import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.realm.ClientConfigBuilder;

/**
 * Configuration of the OAuth client that the test framework creates for {@code @InjectOAuthClient}.
 * It keeps every framework default and only changes the client id.
 * <p>
 * The id must not collide with a client that already exists in the realm. Since Keycloak 26.6.4 the
 * framework registers its client with {@code POST /admin/realms/{realm}/clients} and asserts a 201
 * on the response ({@code ApiUtil.getCreatedId}); 26.2.4 tolerated a 409 instead
 * ({@code ApiUtil.handleCreatedResponse}, since removed). This previously pointed at {@code account},
 * the built-in account-management client present in every realm, which now turns into
 * {@code expected: <201> but was: <409>} during injection, before any test body runs.
 * <p>
 * Using a client of our own is safe here: {@code CtAccountLoader} builds its {@code Auth} from
 * {@code Constants.ACCOUNT_MANAGEMENT_CLIENT_ID} rather than from the token's client, and the
 * {@code auth.require(MANAGE_ACCOUNT)} checks read {@code resource_access.account}, which a
 * full-scope client's token carries for any user holding the default account roles.
 */
public class AccountOAuthClientConfig extends DefaultOAuthClientConfiguration {
    /** Deliberately distinct from every client declared in {@code testrealm.json} and from the built-ins. */
    public static final String CLIENT_ID = "restapi-test-app";

    @Override
    public ClientConfigBuilder configure(ClientConfigBuilder client) {
        return super.configure(client).clientId(CLIENT_ID);
    }
}
