package io.cloudtrust.keycloak.restapi.email.model;

public class BasicMessageModel {
    private String subject;
    private String textMessage;
    private String htmlMessage;

    public BasicMessageModel() {
    }

    public BasicMessageModel(String subject, String textMessage, String htmlMessage) {
        this.subject = subject;
        this.textMessage = textMessage;
        this.htmlMessage = htmlMessage;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTextMessage() {
        return textMessage;
    }

    public void setTextMessage(String message) {
        this.textMessage = message;
    }

    public String getHtmlMessage() {
        return htmlMessage;
    }

    public void setHtmlMessage(String message) {
        this.htmlMessage = message;
    }
}
