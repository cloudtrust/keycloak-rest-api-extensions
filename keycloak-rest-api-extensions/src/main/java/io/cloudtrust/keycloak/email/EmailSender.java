package io.cloudtrust.keycloak.email;

import io.cloudtrust.keycloak.Events;
import io.cloudtrust.keycloak.email.model.EmailModel;
import io.cloudtrust.keycloak.email.provider.FreeMarkerEmailAttachmentsSenderProvider;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.email.EmailException;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Locale;
import java.util.Map;

public class EmailSender {
    public static Response sendMail(KeycloakSession session, RealmModel realm, EmailModel emailModel, Locale locale, Map<String, Object> attributes) {
        if (emailModel.getTheming() == null && emailModel.getBasicMessage() == null) {
            return ErrorResponse.error("Either BasicMessage or Theming configuration should be configured", Response.Status.BAD_REQUEST);
        }
        if (emailModel.getTheming()!=null) {
            if (StringUtils.isBlank(emailModel.getTheming().getTemplate())) {
                return ErrorResponse.error("Template email missing", Response.Status.BAD_REQUEST);
            }
            
            if (StringUtils.isBlank(emailModel.getTheming().getSubjectKey())) {
                return ErrorResponse.error("Subject missing", Response.Status.BAD_REQUEST);
            }
        }

        try {
            new FreeMarkerEmailAttachmentsSenderProvider(session)
                    .setSmtpConfig(realm.getSmtpConfig())
                    .setDefaultLocale(locale)
                    .send(emailModel, attributes);

            String receiverAddress = emailModel.getRecipient();

            EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
            eventBuilder.event(EventType.CUSTOM_REQUIRED_ACTION)
                    .user(session.users().getUserByEmail(realm, receiverAddress))
                    .detail(Events.CT_EVENT_TYPE, "EMAIL_SENT")
                    .detail("receiverAddress", receiverAddress)
                    .detail("themeRealmName", emailModel.getTheming().getThemeRealmName())
                    .detail("locale", emailModel.getTheming().getLocale())
                    .detail("subjectKey", emailModel.getTheming().getSubjectKey())
                    .detail("template", emailModel.getTheming().getTemplate())
                    .success();

            return Response.noContent().build();
        } catch (EmailException e) {
            throw new WebApplicationException("Can't send mail to " + emailModel.getRecipient(), e);
        }
    }
}
