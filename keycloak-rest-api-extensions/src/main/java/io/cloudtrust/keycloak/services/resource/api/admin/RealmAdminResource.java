package io.cloudtrust.keycloak.services.resource.api.admin;

import io.cloudtrust.keycloak.email.EmailSender;
import io.cloudtrust.keycloak.email.model.EmailModel;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RealmAdminResource extends org.keycloak.services.resources.admin.RealmAdminResource {

    private final AdminEventBuilder adminEvent;

    public RealmAdminResource(AdminPermissionEvaluator auth, RealmModel realm, TokenManager tokenManager,
                              AdminEventBuilder adminEvent, KeycloakSession session) {
        super(auth, realm, tokenManager, adminEvent);
        this.adminEvent = adminEvent;
        this.session = session;
    }

    @Path("users")
    @Override
    public UsersResource users() {
        UsersResource users = new UsersResource(session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(users);
        //resourceContext.initResource(users);
        return users;
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
            return ErrorResponse.error("Recipient email missing", Response.Status.BAD_REQUEST);
        }

        Locale locale = realm.getDefaultLocale() != null ? Locale.forLanguageTag(realm.getDefaultLocale()) : Locale.ENGLISH;
        UriBuilder builder = LoginActionsService.loginActionsBaseUrl(session.getContext().getUri());
        String link = builder.build(session.getContext().getRealm().getName()).toString() + "/";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("link", link);
        if (emailModel.getTheming() != null && emailModel.getTheming().getTemplateParameters() != null) {
            emailModel.getTheming().getTemplateParameters().forEach(attributes::put);
        }

        return EmailSender.sendMail(this.session, this.realm, emailModel, locale, attributes);
    }
}
