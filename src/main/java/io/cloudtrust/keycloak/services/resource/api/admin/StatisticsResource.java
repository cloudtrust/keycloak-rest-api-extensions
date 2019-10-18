package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.representations.idm.CredentialsStatisticsRepresentation;
import io.cloudtrust.keycloak.representations.idm.UsersStatisticsRepresentation;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public class StatisticsResource {

    private static final Logger logger = Logger.getLogger(StatisticsResource.class);

    private AdminPermissionEvaluator auth;
    private KeycloakSession session;
    private RealmModel realm;

    public StatisticsResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.auth = auth;
        this.session = session;
        this.realm = session.getContext().getRealm();
    }

    @Path("users")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UsersStatisticsRepresentation getUsersStatistics() {
        auth.users().requireView();
        return new UsersStatisticsRepresentation(42, 12, 3);
    }

    @Path("credentials")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public CredentialsStatisticsRepresentation getCredentialsStatistics() {
        auth.users().requireView();
        CredentialsStatisticsRepresentation asr = new CredentialsStatisticsRepresentation();
        asr.put("otp", 8);
        asr.put("ctopt", 2);
        asr.put("ctpapercard", 2);
        return asr;
    }

}
