package io.cloudtrust.keycloak.services.resource.api.admin;


import org.keycloak.models.KeycloakSession;

import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import jakarta.ws.rs.Path;


public class CtRealmAdminResource {
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final KeycloakSession session;

    public CtRealmAdminResource(AdminPermissionEvaluator auth, AdminEventBuilder adminEvent, KeycloakSession session) {
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.session = session;
    }

    @Path("users")
    public CtUsersResource users() {
        return new CtUsersResource(session, auth, adminEvent);
    }

    @Path("statistics")
    public StatisticsResource statistics() {
        return new StatisticsResource(session, auth);
    }
}
