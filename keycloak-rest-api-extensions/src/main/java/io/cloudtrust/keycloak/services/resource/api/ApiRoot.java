package io.cloudtrust.keycloak.services.resource.api;

import io.cloudtrust.keycloak.services.resource.api.account.AccountLoader;
import io.cloudtrust.keycloak.services.resource.api.admin.AdminRoot;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

public class ApiRoot {

    private KeycloakSession session;

    public ApiRoot(KeycloakSession session) {
        this.session = session;
    }

    /**
     * @return The admin API
     */
    @Path("admin")
    public Object getAdminApiRoot() {
        return new AdminRoot(session);
    }


    @Path("account/realms/{realm}")
    public Object getAccountApiRoot(final @PathParam("realm") String name) {
        RealmModel realm = init(name);
        EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection());
        return new AccountLoader().getAccountService(session, event);
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
