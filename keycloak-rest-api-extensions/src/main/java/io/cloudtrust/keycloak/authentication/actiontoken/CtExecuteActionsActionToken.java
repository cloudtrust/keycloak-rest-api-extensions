package io.cloudtrust.keycloak.authentication.actiontoken;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.authentication.actiontoken.DefaultActionToken;

import java.util.LinkedList;
import java.util.List;

/**
 * Inspired by Keycloak ExecuteActionsActionToken
 */
public class CtExecuteActionsActionToken extends DefaultActionToken {
    public static final String TOKEN_TYPE = "ct-execute-actions";

    private static final String CLAIM_EMAIL = "etv";
    private static final String JSON_FIELD_REQUIRED_ACTIONS = "ctrqac";
    private static final String JSON_FIELD_REDIRECT_URI = "reduri";

    public CtExecuteActionsActionToken(String userId, int absoluteExpirationInSecs, List<String> requiredActions, String redirectUri, String clientId) {
        super(userId, TOKEN_TYPE, absoluteExpirationInSecs, null);
        setRequiredActions(requiredActions == null ? new LinkedList<>() : new LinkedList<>(requiredActions));
        setRedirectUri(redirectUri);
        this.issuedFor = clientId;
    }

    public CtExecuteActionsActionToken(String userId, String email, int absoluteExpirationInSecs, List<String> requiredActions, String redirectUri, String clientId) {
        this(userId, absoluteExpirationInSecs, requiredActions, redirectUri, clientId);
        setEmail(email);
    }

    // Used by JSON deserialization
    private CtExecuteActionsActionToken() {
    }

    @JsonProperty(value = JSON_FIELD_REQUIRED_ACTIONS)
    public List<String> getRequiredActions() {
        return (List<String>) getOtherClaims().get(JSON_FIELD_REQUIRED_ACTIONS);
    }

    @JsonProperty(value = JSON_FIELD_REQUIRED_ACTIONS)
    public final void setRequiredActions(List<String> requiredActions) {
        if (requiredActions == null) {
            getOtherClaims().remove(JSON_FIELD_REQUIRED_ACTIONS);
        } else {
            setOtherClaims(JSON_FIELD_REQUIRED_ACTIONS, requiredActions);
        }
    }

    @JsonProperty(value = JSON_FIELD_REDIRECT_URI)
    public String getRedirectUri() {
        return (String) getOtherClaims().get(JSON_FIELD_REDIRECT_URI);
    }

    @JsonProperty(value = JSON_FIELD_REDIRECT_URI)
    public final void setRedirectUri(String redirectUri) {
        if (redirectUri == null) {
            getOtherClaims().remove(JSON_FIELD_REDIRECT_URI);
        } else {
            setOtherClaims(JSON_FIELD_REDIRECT_URI, redirectUri);
        }
    }

    @JsonIgnore
    public String getEmailToValidate() {
        return (String) this.getOtherClaims().get(CLAIM_EMAIL);
    }

    @JsonIgnore
    public void setEmailToValidate(String email) {
        this.getOtherClaims().put(CLAIM_EMAIL, email);
    }
}
