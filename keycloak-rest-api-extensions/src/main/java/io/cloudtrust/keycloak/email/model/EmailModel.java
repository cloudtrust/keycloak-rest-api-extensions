package io.cloudtrust.keycloak.email.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailModel {
    private String recipient;
    private BasicMessageModel basicMessage;
    private ThemingModel theming;
    private List<AttachmentModel> attachments;

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public List<AttachmentModel> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentModel> attachments) {
        this.attachments = attachments;
    }

    public BasicMessageModel getBasicMessage() {
        return this.basicMessage;
    }

    public void setBasicMessage(BasicMessageModel basicMessage) {
        this.basicMessage = basicMessage;
    }

    public ThemingModel getTheming() {
        return theming;
    }

    public void setTheming(ThemingModel theming) {
        this.theming = theming;
    }
}
