package io.cloudtrust.keycloak.services.resource.api;

import io.cloudtrust.keycloak.services.resource.api.account.CtAccountLoader;
import io.cloudtrust.keycloak.services.resource.api.admin.CtAdminRoot;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;

public class ApiRoot {
    private final KeycloakSession session;
    private final ApiConfig apiConfig;

    public ApiRoot(KeycloakSession session, ApiConfig apiConfig) {
        this.session = session;
        this.apiConfig = apiConfig;
    }

    /**
     * @return The admin API
     */
    @Path("admin")
    public Object getAdminApiRoot() {
        return new CtAdminRoot(session, apiConfig);
    }


    @Path("account/realms/{realm}")
    public Object getAccountApiRoot(final @PathParam("realm") String name) {
        RealmModel realm = init(name);
        EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection());
        return new CtAccountLoader(session, event).getAccountService();
    }

    private RealmModel init(String realmName) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotFoundException("Realm does not exist");
        }
        session.getContext().setRealm(realm);
        return realm;
    }
}
