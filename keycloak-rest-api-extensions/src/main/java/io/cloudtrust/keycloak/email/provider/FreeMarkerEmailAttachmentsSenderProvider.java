package io.cloudtrust.keycloak.email.provider;

import com.sun.mail.smtp.SMTPMessage;
import io.cloudtrust.keycloak.email.model.AttachmentModel;
import io.cloudtrust.keycloak.email.model.BasicMessageModel;
import io.cloudtrust.keycloak.email.model.EmailModel;
import io.cloudtrust.keycloak.email.model.ThemingModel;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ServicesLogger;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.MessageFormatterMethod;
import org.keycloak.theme.freemarker.FreeMarkerProvider;
import org.keycloak.truststore.HostnameVerificationPolicy;
import org.keycloak.truststore.JSSETruststoreConfigurator;
import org.keycloak.vault.VaultStringSecret;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Inspired by FreeMarkerEmailTemplateProvider and DefaultEmailSenderProvider
 *
 * @author fpe
 */
public class FreeMarkerEmailAttachmentsSenderProvider {
    private static final Logger logger = Logger.getLogger(FreeMarkerEmailAttachmentsSenderProvider.class);

    private final KeycloakSession session;
    private final FreeMarkerProvider freeMarker;
    private Map<String, String> smtpConfig;
    private Locale defaultLocale;

    public FreeMarkerEmailAttachmentsSenderProvider(KeycloakSession session) {
        this(session, session.getProvider(FreeMarkerProvider.class));
    }

    public FreeMarkerEmailAttachmentsSenderProvider(KeycloakSession session, FreeMarkerProvider freeMarker) {
        this.session = session;
        this.freeMarker = freeMarker;
    }

    public FreeMarkerEmailAttachmentsSenderProvider setSmtpConfig(Map<String, String> smtpConfig) {
        this.smtpConfig = smtpConfig;
        return this;
    }

    public FreeMarkerEmailAttachmentsSenderProvider setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
        return this;
    }

    public void send(EmailModel email, Map<String, Object> attributes) throws EmailException {
        try {
            BasicMessageModel message = email.getBasicMessage();
            if (message == null) {
                message = processTemplate(email.getTheming(), attributes);
            }
            if (message.getTextMessage() == null && message.getHtmlMessage() == null) {
                throw new NullPointerException("Missing both text and html messages");
            }
            this.send(email.getRecipient(), message, email.getAttachments());
        } catch (EmailException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailException("Failed to template email", e);
        }
    }

    protected BasicMessageModel processTemplate(ThemingModel theming, Map<String, Object> attributes) throws EmailException {
        try {
            Locale locale = defaultLocale;
            if (theming.getLocale() != null) {
                locale = Locale.forLanguageTag(theming.getLocale());
            }
            Theme theme = getTheme();
            attributes.put("locale", locale);
            Properties rb = theme.getMessages(locale);
            attributes.put("msg", new MessageFormatterMethod(locale, rb));
            attributes.put("properties", theme.getProperties());
            String subject = new MessageFormat(rb.getProperty(theming.getSubjectKey(), theming.getSubjectKey()), locale).format(theming.getSubjectParametersAsArray());
            String textBody = loadTemplate(attributes, "text", theming.getTemplate(), theme);
            String htmlBody = loadTemplate(attributes, "html", theming.getTemplate(), theme);

            return new BasicMessageModel(subject, textBody, htmlBody);
        } catch (Exception e) {
            throw new EmailException("Failed to template email", e);
        }
    }

    private String loadTemplate(Map<String, Object> attributes, String type, String template, Theme theme) {
        String templateRef = String.format("%s/%s", type, template);
        try {
            return freeMarker.processTemplate(attributes, templateRef, theme);
        } catch (final FreeMarkerException e) {
            logger.error("Failed to load template "+templateRef, e);
            return null;
        }
    }

    protected Theme getTheme() throws IOException {
        return session.theme().getTheme(Theme.Type.EMAIL);
    }

    private boolean isTrue(Map<String, String> config, String param) {
        return "true".equals(config.get(param));
    }

    protected InternetAddress toInternetAddress(String email, String displayName) throws UnsupportedEncodingException, AddressException, EmailException {
        if (StringUtils.isBlank(email)) {
            throw new EmailException("Please provide a valid address", null);
        }
        if (StringUtils.isBlank(displayName)) {
            return new InternetAddress(email);
        }
        return new InternetAddress(email, displayName, "utf-8");
    }

    private void setupTruststore(Properties props) {
        JSSETruststoreConfigurator configurator = new JSSETruststoreConfigurator(session);

        SSLSocketFactory factory = configurator.getSSLSocketFactory();
        if (factory != null) {
            props.put("mail.smtp.ssl.socketFactory", factory);
            if (configurator.getProvider().getPolicy() == HostnameVerificationPolicy.ANY) {
                props.setProperty("mail.smtp.ssl.trust", "*");
            }
        }
    }

    public void send(String address, BasicMessageModel message, List<AttachmentModel> attachments) throws EmailException {
        try {
            Properties props = createProperties(smtpConfig);

            Session smtpSession = Session.getDefaultInstance(props);

            // In case of further evolution of this code, check this link:
            // stackoverflow.com/questions/3902455/mail-multipart-alternative-vs-multipart-mixed/23853079

            // Create mixed mime multipart as the global envelop
            final Multipart mpMixed = new MimeMultipart("mixed");
            mpMixed.addBodyPart(createMessageBodyPart(message));
            if (attachments != null) {
                for (AttachmentModel attachment : attachments) {
                    mpMixed.addBodyPart(createAttachmentBodyPart(attachment));
                }
            }

            MimeMessage msg = createSMTPMessage(smtpSession, smtpConfig, address, message.getSubject());
            msg.setContent(mpMixed);
            msg.saveChanges();
            msg.setSentDate(new Date());

            try (Transport transport = smtpSession.getTransport("smtp")) {
                if (isTrue(smtpConfig, "auth")) {
                    try (VaultStringSecret vaultStringSecret = this.session.vault().getStringSecret(smtpConfig.get("password"))) {
                        transport.connect(smtpConfig.get("user"), vaultStringSecret.get().orElse(smtpConfig.get("password")));
                    }
                } else {
                    transport.connect();
                }
                transport.sendMessage(msg, new InternetAddress[]{new InternetAddress(address)});
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
            throw new EmailException(e);
        }
    }

    private MimeMessage createSMTPMessage(Session smtpSession, Map<String, String> config, String address, String subject) throws UnsupportedEncodingException, MessagingException, EmailException {
        String from = config.get("from");
        String fromDisplayName = config.get("fromDisplayName");
        String replyTo = config.get("replyTo");
        String replyToDisplayName = config.get("replyToDisplayName");
        String envelopeFrom = config.get("envelopeFrom");

        SMTPMessage msg = new SMTPMessage(smtpSession);
        msg.setFrom(toInternetAddress(from, fromDisplayName));

        msg.setReplyTo(new Address[]{toInternetAddress(from, fromDisplayName)});
        if (replyTo != null && !replyTo.isEmpty()) {
            msg.setReplyTo(new Address[]{toInternetAddress(replyTo, replyToDisplayName)});
        }
        if (envelopeFrom != null && !envelopeFrom.isEmpty()) {
            msg.setEnvelopeFrom(envelopeFrom);
        }

        msg.setHeader("To", address);
        msg.setSubject(subject, "utf-8");

        return msg;
    }

    private MimeBodyPart createMessageBodyPart(BasicMessageModel message) throws MessagingException {
        Multipart multipart = new MimeMultipart("alternative");

        if (message.getTextMessage() != null) {
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(message.getTextMessage(), "UTF-8");
            multipart.addBodyPart(textPart);
        }

        if (message.getHtmlMessage() != null) {
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(message.getHtmlMessage(), "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);
        }

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setContent(multipart);

        return mbp;
    }

    private MimeBodyPart createAttachmentBodyPart(AttachmentModel attachment) throws MessagingException {
        DataSource htmlPartImgDs = new ByteArrayDataSource(attachment.getContent(), attachment.getContentType());

        final MimeBodyPart mbpAttachment = new MimeBodyPart();
        mbpAttachment.setDataHandler(new DataHandler(htmlPartImgDs));
        mbpAttachment.setDisposition(Part.ATTACHMENT);
        mbpAttachment.setFileName(attachment.getFilename());

        return mbpAttachment;
    }

    private Properties createProperties(Map<String, String> config) {
        Properties props = new Properties();

        if (config.containsKey("host")) {
            props.setProperty("mail.smtp.host", config.get("host"));
        }

        boolean auth = isTrue(config, "auth");
        boolean ssl = isTrue(config, "ssl");
        boolean starttls = isTrue(config, "starttls");

        if (config.get("port") != null) {
            props.setProperty("mail.smtp.port", config.get("port"));
        }

        if (auth) {
            props.setProperty("mail.smtp.auth", "true");
        }

        if (ssl) {
            props.setProperty("mail.smtp.ssl.enable", "true");
        }

        if (starttls) {
            props.setProperty("mail.smtp.starttls.enable", "true");
        }

        if (ssl || starttls) {
            setupTruststore(props);
        }

        props.setProperty("mail.smtp.timeout", "10000");
        props.setProperty("mail.smtp.connectiontimeout", "10000");

        return props;
    }
}
