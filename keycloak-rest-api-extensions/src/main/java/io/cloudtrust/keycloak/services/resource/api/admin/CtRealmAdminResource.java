package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.email.EmailSender;
import io.cloudtrust.keycloak.email.model.EmailModel;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    @Path("send-email")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendMail(EmailModel emailModel) {
        auth.users().requireManage();

        if (StringUtils.isBlank(emailModel.getRecipient())) {
            throw ErrorResponse.error("Recipient email missing", Response.Status.BAD_REQUEST);
        }

        RealmModel realm = this.session.getContext().getRealm();
        Locale locale = realm.getDefaultLocale() != null ? Locale.forLanguageTag(realm.getDefaultLocale()) : Locale.ENGLISH;
        UriBuilder builder = LoginActionsService.loginActionsBaseUrl(session.getContext().getUri());
        String link = builder.build(session.getContext().getRealm().getName()).toString() + "/";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("link", link);
        if (emailModel.getTheming() != null && emailModel.getTheming().getTemplateParameters() != null) {
            emailModel.getTheming().getTemplateParameters().forEach(attributes::put);
        }

        return EmailSender.sendMail(this.session, emailModel, locale, attributes);
    }
}
