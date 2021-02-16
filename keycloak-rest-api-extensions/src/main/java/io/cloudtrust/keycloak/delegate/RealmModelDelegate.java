package io.cloudtrust.keycloak.delegate;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.WebAuthnPolicy;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RealmModelDelegate implements RealmModel {
    private RealmModel delegate;

    public RealmModelDelegate(RealmModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public RoleModel getRole(String name) {
        return delegate.getRole(name);
    }

    @Override
    public RoleModel addRole(String name) {
        return delegate.addRole(name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return delegate.addRole(id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return delegate.removeRole(role);
    }

    @Override
    public Set<RoleModel> getRoles() {
        return delegate.getRoles();
    }

    @Override
    public List<String> getDefaultRoles() {
        return delegate.getDefaultRoles();
    }

    @Override
    public void addDefaultRole(String name) {
        delegate.addDefaultRole(name);
    }

    @Override
    public void updateDefaultRoles(String... defaultRoles) {
        delegate.updateDefaultRoles(defaultRoles);
    }

    @Override
    public void removeDefaultRoles(String... defaultRoles) {
        delegate.removeDefaultRoles(defaultRoles);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public String getDisplayName() {
        return delegate.getDisplayName();
    }

    @Override
    public void setDisplayName(String displayName) {
        delegate.setDisplayName(displayName);
    }

    @Override
    public String getDisplayNameHtml() {
        return delegate.getDisplayNameHtml();
    }

    @Override
    public void setDisplayNameHtml(String displayNameHtml) {
        delegate.setDisplayNameHtml(displayNameHtml);
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    @Override
    public SslRequired getSslRequired() {
        return delegate.getSslRequired();
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        delegate.setSslRequired(sslRequired);
    }

    @Override
    public boolean isRegistrationAllowed() {
        return delegate.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        delegate.setRegistrationAllowed(registrationAllowed);
    }

    @Override
    public boolean isRegistrationEmailAsUsername() {
        return delegate.isRegistrationEmailAsUsername();
    }

    @Override
    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        delegate.setRegistrationEmailAsUsername(registrationEmailAsUsername);
    }

    @Override
    public boolean isRememberMe() {
        return delegate.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        delegate.setRememberMe(rememberMe);
    }

    @Override
    public boolean isEditUsernameAllowed() {
        return delegate.isEditUsernameAllowed();
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        delegate.setEditUsernameAllowed(editUsernameAllowed);
    }

    @Override
    public boolean isUserManagedAccessAllowed() {
        return delegate.isUserManagedAccessAllowed();
    }

    @Override
    public void setUserManagedAccessAllowed(boolean userManagedAccessAllowed) {
        delegate.setUserManagedAccessAllowed(userManagedAccessAllowed);
    }

    @Override
    public void setAttribute(String name, String value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, Boolean value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, Integer value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, Long value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public Integer getAttribute(String name, Integer defaultValue) {
        return delegate.getAttribute(name, defaultValue);
    }

    @Override
    public Long getAttribute(String name, Long defaultValue) {
        return delegate.getAttribute(name, defaultValue);
    }

    @Override
    public Boolean getAttribute(String name, Boolean defaultValue) {
        return delegate.getAttribute(name, defaultValue);
    }

    @Override
    public Map<String, String> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public boolean isBruteForceProtected() {
        return delegate.isBruteForceProtected();
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        delegate.setBruteForceProtected(value);
    }

    @Override
    public boolean isPermanentLockout() {
        return delegate.isPermanentLockout();
    }

    @Override
    public void setPermanentLockout(boolean val) {
        delegate.setPermanentLockout(val);
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        return delegate.getMaxFailureWaitSeconds();
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        delegate.setMaxFailureWaitSeconds(val);
    }

    @Override
    public int getWaitIncrementSeconds() {
        return delegate.getWaitIncrementSeconds();
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        delegate.setWaitIncrementSeconds(val);
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        return delegate.getMinimumQuickLoginWaitSeconds();
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        delegate.setMinimumQuickLoginWaitSeconds(val);
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        return delegate.getQuickLoginCheckMilliSeconds();
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        delegate.setQuickLoginCheckMilliSeconds(val);
    }

    @Override
    public int getMaxDeltaTimeSeconds() {
        return delegate.getMaxDeltaTimeSeconds();
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        delegate.setMaxDeltaTimeSeconds(val);
    }

    @Override
    public int getFailureFactor() {
        return delegate.getFailureFactor();
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        delegate.setFailureFactor(failureFactor);
    }

    @Override
    public boolean isVerifyEmail() {
        return delegate.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        delegate.setVerifyEmail(verifyEmail);
    }

    @Override
    public boolean isLoginWithEmailAllowed() {
        return delegate.isLoginWithEmailAllowed();
    }

    @Override
    public void setLoginWithEmailAllowed(boolean loginWithEmailAllowed) {
        delegate.setLoginWithEmailAllowed(loginWithEmailAllowed);
    }

    @Override
    public boolean isDuplicateEmailsAllowed() {
        return delegate.isDuplicateEmailsAllowed();
    }

    @Override
    public void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed) {
        delegate.setDuplicateEmailsAllowed(duplicateEmailsAllowed);
    }

    @Override
    public boolean isResetPasswordAllowed() {
        return delegate.isResetPasswordAllowed();
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        delegate.setResetPasswordAllowed(resetPasswordAllowed);
    }

    @Override
    public String getDefaultSignatureAlgorithm() {
        return delegate.getDefaultSignatureAlgorithm();
    }

    @Override
    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        delegate.setDefaultSignatureAlgorithm(defaultSignatureAlgorithm);
    }

    @Override
    public boolean isRevokeRefreshToken() {
        return delegate.isRevokeRefreshToken();
    }

    @Override
    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        delegate.setRevokeRefreshToken(revokeRefreshToken);
    }

    @Override
    public int getRefreshTokenMaxReuse() {
        return delegate.getRefreshTokenMaxReuse();
    }

    @Override
    public void setRefreshTokenMaxReuse(int revokeRefreshTokenCount) {
        delegate.setRefreshTokenMaxReuse(revokeRefreshTokenCount);
    }

    @Override
    public int getSsoSessionIdleTimeout() {
        return delegate.getSsoSessionIdleTimeout();
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        delegate.setSsoSessionIdleTimeout(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        return delegate.getSsoSessionMaxLifespan();
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        delegate.setSsoSessionMaxLifespan(seconds);
    }

    @Override
    public int getSsoSessionIdleTimeoutRememberMe() {
        return delegate.getSsoSessionIdleTimeoutRememberMe();
    }

    @Override
    public void setSsoSessionIdleTimeoutRememberMe(int seconds) {
        delegate.setSsoSessionIdleTimeoutRememberMe(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespanRememberMe() {
        return delegate.getSsoSessionMaxLifespanRememberMe();
    }

    @Override
    public void setSsoSessionMaxLifespanRememberMe(int seconds) {
        delegate.setSsoSessionMaxLifespanRememberMe(seconds);
    }

    @Override
    public int getOfflineSessionIdleTimeout() {
        return delegate.getOfflineSessionIdleTimeout();
    }

    @Override
    public void setOfflineSessionIdleTimeout(int seconds) {
        delegate.setOfflineSessionIdleTimeout(seconds);
    }

    @Override
    public int getAccessTokenLifespan() {
        return delegate.getAccessTokenLifespan();
    }

    @Override
    public boolean isOfflineSessionMaxLifespanEnabled() {
        return delegate.isOfflineSessionMaxLifespanEnabled();
    }

    @Override
    public void setOfflineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {
        delegate.setOfflineSessionMaxLifespanEnabled(offlineSessionMaxLifespanEnabled);
    }

    @Override
    public int getOfflineSessionMaxLifespan() {
        return delegate.getOfflineSessionMaxLifespan();
    }

    @Override
    public void setOfflineSessionMaxLifespan(int seconds) {
        delegate.setOfflineSessionMaxLifespan(seconds);
    }

    @Override
    public void setAccessTokenLifespan(int seconds) {
        delegate.setAccessTokenLifespan(seconds);
    }

    @Override
    public int getAccessTokenLifespanForImplicitFlow() {
        return delegate.getAccessTokenLifespanForImplicitFlow();
    }

    @Override
    public void setAccessTokenLifespanForImplicitFlow(int seconds) {
        delegate.setAccessTokenLifespanForImplicitFlow(seconds);
    }

    @Override
    public int getAccessCodeLifespan() {
        return delegate.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(int seconds) {
        delegate.setAccessCodeLifespan(seconds);
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        return delegate.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(int seconds) {
        delegate.setAccessCodeLifespanUserAction(seconds);
    }

    @Override
    public Map<String, Integer> getUserActionTokenLifespans() {
        return delegate.getUserActionTokenLifespans();
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        return delegate.getAccessCodeLifespanLogin();
    }

    @Override
    public void setAccessCodeLifespanLogin(int seconds) {
        delegate.setAccessCodeLifespanLogin(seconds);
    }

    @Override
    public int getActionTokenGeneratedByAdminLifespan() {
        return delegate.getActionTokenGeneratedByAdminLifespan();
    }

    @Override
    public void setActionTokenGeneratedByAdminLifespan(int seconds) {
        delegate.setActionTokenGeneratedByAdminLifespan(seconds);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan() {
        return delegate.getActionTokenGeneratedByUserLifespan();
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(int seconds) {
        delegate.setActionTokenGeneratedByUserLifespan(seconds);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan(String actionTokenType) {
        return delegate.getActionTokenGeneratedByUserLifespan(actionTokenType);
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(String actionTokenType, Integer seconds) {
        delegate.setActionTokenGeneratedByUserLifespan(actionTokenType, seconds);
    }

    @Override
    public List<RequiredCredentialModel> getRequiredCredentials() {
        return delegate.getRequiredCredentials();
    }

    @Override
    public void addRequiredCredential(String cred) {
        delegate.addRequiredCredential(cred);
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        return delegate.getPasswordPolicy();
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        delegate.setPasswordPolicy(policy);
    }

    @Override
    public OTPPolicy getOTPPolicy() {
        return delegate.getOTPPolicy();
    }

    @Override
    public void setOTPPolicy(OTPPolicy policy) {
        delegate.setOTPPolicy(policy);
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicy() {
        return delegate.getWebAuthnPolicy();
    }

    @Override
    public void setWebAuthnPolicy(WebAuthnPolicy policy) {
        delegate.setWebAuthnPolicy(policy);
    }

    @Override
    public RoleModel getRoleById(String id) {
        return delegate.getRoleById(id);
    }

    @Override
    public List<GroupModel> getDefaultGroups() {
        return delegate.getDefaultGroups();
    }

    @Override
    public void addDefaultGroup(GroupModel group) {
        delegate.addDefaultGroup(group);
    }

    @Override
    public void removeDefaultGroup(GroupModel group) {
        delegate.removeDefaultGroup(group);
    }

    @Override
    public List<ClientModel> getClients() {
        return delegate.getClients();
    }

    @Override
    public ClientModel addClient(String name) {
        return delegate.addClient(name);
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        return delegate.addClient(id, clientId);
    }

    @Override
    public boolean removeClient(String id) {
        return delegate.removeClient(id);
    }

    @Override
    public ClientModel getClientById(String id) {
        return delegate.getClientById(id);
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        return delegate.getClientByClientId(clientId);
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        delegate.updateRequiredCredentials(creds);
    }

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        return delegate.getBrowserSecurityHeaders();
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        delegate.setBrowserSecurityHeaders(headers);
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        return delegate.getSmtpConfig();
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        delegate.setSmtpConfig(smtpConfig);
    }

    @Override
    public AuthenticationFlowModel getBrowserFlow() {
        return delegate.getBrowserFlow();
    }

    @Override
    public void setBrowserFlow(AuthenticationFlowModel flow) {
        delegate.setBrowserFlow(flow);
    }

    @Override
    public AuthenticationFlowModel getRegistrationFlow() {
        return delegate.getRegistrationFlow();
    }

    @Override
    public void setRegistrationFlow(AuthenticationFlowModel flow) {
        delegate.setRegistrationFlow(flow);
    }

    @Override
    public AuthenticationFlowModel getDirectGrantFlow() {
        return delegate.getDirectGrantFlow();
    }

    @Override
    public void setDirectGrantFlow(AuthenticationFlowModel flow) {
        delegate.setDirectGrantFlow(flow);
    }

    @Override
    public AuthenticationFlowModel getResetCredentialsFlow() {
        return delegate.getResetCredentialsFlow();
    }

    @Override
    public void setResetCredentialsFlow(AuthenticationFlowModel flow) {
        delegate.setResetCredentialsFlow(flow);
    }

    @Override
    public AuthenticationFlowModel getClientAuthenticationFlow() {
        return delegate.getClientAuthenticationFlow();
    }

    @Override
    public void setClientAuthenticationFlow(AuthenticationFlowModel flow) {
        delegate.setClientAuthenticationFlow(flow);
    }

    @Override
    public AuthenticationFlowModel getDockerAuthenticationFlow() {
        return delegate.getDockerAuthenticationFlow();
    }

    @Override
    public void setDockerAuthenticationFlow(AuthenticationFlowModel flow) {
        delegate.setDockerAuthenticationFlow(flow);
    }

    @Override
    public List<AuthenticationFlowModel> getAuthenticationFlows() {
        return delegate.getAuthenticationFlows();
    }

    @Override
    public AuthenticationFlowModel getFlowByAlias(String alias) {
        return delegate.getFlowByAlias(alias);
    }

    @Override
    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        return delegate.addAuthenticationFlow(model);
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String id) {
        return delegate.getAuthenticationFlowById(id);
    }

    @Override
    public void removeAuthenticationFlow(AuthenticationFlowModel model) {
        delegate.removeAuthenticationFlow(model);
    }

    @Override
    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        delegate.updateAuthenticationFlow(model);
    }

    @Override
    public List<AuthenticationExecutionModel> getAuthenticationExecutions(String flowId) {
        return delegate.getAuthenticationExecutions(flowId);
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        return delegate.getAuthenticationExecutionById(id);
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId) {
        return delegate.getAuthenticationExecutionByFlowId(flowId);
    }

    @Override
    public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
        return delegate.addAuthenticatorExecution(model);
    }

    @Override
    public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {
        delegate.updateAuthenticatorExecution(model);
    }

    @Override
    public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {
        delegate.removeAuthenticatorExecution(model);
    }

    @Override
    public List<AuthenticatorConfigModel> getAuthenticatorConfigs() {
        return delegate.getAuthenticatorConfigs();
    }

    @Override
    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        return delegate.addAuthenticatorConfig(model);
    }

    @Override
    public void updateAuthenticatorConfig(AuthenticatorConfigModel model) {
        delegate.updateAuthenticatorConfig(model);
    }

    @Override
    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        delegate.removeAuthenticatorConfig(model);
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        return delegate.getAuthenticatorConfigById(id);
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias) {
        return delegate.getAuthenticatorConfigByAlias(alias);
    }

    @Override
    public List<RequiredActionProviderModel> getRequiredActionProviders() {
        return delegate.getRequiredActionProviders();
    }

    @Override
    public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
        return delegate.addRequiredActionProvider(model);
    }

    @Override
    public void updateRequiredActionProvider(RequiredActionProviderModel model) {
        delegate.updateRequiredActionProvider(model);
    }

    @Override
    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        delegate.removeRequiredActionProvider(model);
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderById(String id) {
        return delegate.getRequiredActionProviderById(id);
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        return delegate.getRequiredActionProviderByAlias(alias);
    }

    @Override
    public List<IdentityProviderModel> getIdentityProviders() {
        return delegate.getIdentityProviders();
    }

    @Override
    public IdentityProviderModel getIdentityProviderByAlias(String alias) {
        return delegate.getIdentityProviderByAlias(alias);
    }

    @Override
    public void addIdentityProvider(IdentityProviderModel identityProvider) {
        delegate.addIdentityProvider(identityProvider);
    }

    @Override
    public void removeIdentityProviderByAlias(String alias) {
        delegate.removeIdentityProviderByAlias(alias);
    }

    @Override
    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        delegate.updateIdentityProvider(identityProvider);
    }

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappers() {
        return delegate.getIdentityProviderMappers();
    }

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(String brokerAlias) {
        return delegate.getIdentityProviderMappersByAlias(brokerAlias);
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model) {
        return delegate.addIdentityProviderMapper(model);
    }

    @Override
    public void removeIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        delegate.removeIdentityProviderMapper(mapping);
    }

    @Override
    public void updateIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        delegate.updateIdentityProviderMapper(mapping);
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(String id) {
        return delegate.getIdentityProviderMapperById(id);
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(String brokerAlias, String name) {
        return delegate.getIdentityProviderMapperByName(brokerAlias, name);
    }

    @Override
    public ComponentModel addComponentModel(ComponentModel model) {
        return delegate.addComponentModel(model);
    }

    @Override
    public ComponentModel importComponentModel(ComponentModel model) {
        return delegate.importComponentModel(model);
    }

    @Override
    public void updateComponent(ComponentModel component) {
        delegate.updateComponent(component);
    }

    @Override
    public void removeComponent(ComponentModel component) {
        delegate.removeComponent(component);
    }

    @Override
    public void removeComponents(String parentId) {
        delegate.removeComponents(parentId);
    }

    @Override
    public List<ComponentModel> getComponents(String parentId, String providerType) {
        return delegate.getComponents(parentId, providerType);
    }

    @Override
    public List<ComponentModel> getComponents(String parentId) {
        return delegate.getComponents(parentId);
    }

    @Override
    public List<ComponentModel> getComponents() {
        return delegate.getComponents();
    }

    @Override
    public ComponentModel getComponent(String id) {
        return delegate.getComponent(id);
    }

    @Override
    public String getLoginTheme() {
        return delegate.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        delegate.setLoginTheme(name);
    }

    @Override
    public String getAccountTheme() {
        return delegate.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        delegate.setAccountTheme(name);
    }

    @Override
    public String getAdminTheme() {
        return delegate.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        delegate.setAdminTheme(name);
    }

    @Override
    public String getEmailTheme() {
        return delegate.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        delegate.setEmailTheme(name);
    }

    @Override
    public int getNotBefore() {
        return delegate.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        delegate.setNotBefore(notBefore);
    }

    @Override
    public boolean isEventsEnabled() {
        return delegate.isEventsEnabled();
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        delegate.setEventsEnabled(enabled);
    }

    @Override
    public long getEventsExpiration() {
        return delegate.getEventsExpiration();
    }

    @Override
    public void setEventsExpiration(long expiration) {
        delegate.setEventsExpiration(expiration);
    }

    @Override
    public Set<String> getEventsListeners() {
        return delegate.getEventsListeners();
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        delegate.setEventsListeners(listeners);
    }

    @Override
    public Set<String> getEnabledEventTypes() {
        return delegate.getEnabledEventTypes();
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        delegate.setEnabledEventTypes(enabledEventTypes);
    }

    @Override
    public boolean isAdminEventsEnabled() {
        return delegate.isAdminEventsEnabled();
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        delegate.setAdminEventsEnabled(enabled);
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        return delegate.isAdminEventsDetailsEnabled();
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        delegate.setAdminEventsDetailsEnabled(enabled);
    }

    @Override
    public ClientModel getMasterAdminClient() {
        return delegate.getMasterAdminClient();
    }

    @Override
    public void setMasterAdminClient(ClientModel client) {
        delegate.setMasterAdminClient(client);
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        return delegate.isIdentityFederationEnabled();
    }

    @Override
    public boolean isInternationalizationEnabled() {
        return delegate.isInternationalizationEnabled();
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        delegate.setInternationalizationEnabled(enabled);
    }

    @Override
    public Set<String> getSupportedLocales() {
        return delegate.getSupportedLocales();
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        delegate.setSupportedLocales(locales);
    }

    @Override
    public String getDefaultLocale() {
        return delegate.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String locale) {
        delegate.setDefaultLocale(locale);
    }

    @Override
    public GroupModel createGroup(String name) {
        return delegate.createGroup(name);
    }

    @Override
    public GroupModel createGroup(String id, String name) {
        return delegate.createGroup(id, name);
    }

    @Override
    public GroupModel getGroupById(String id) {
        return delegate.getGroupById(id);
    }

    @Override
    public List<GroupModel> getGroups() {
        return delegate.getGroups();
    }

    @Override
    public Long getGroupsCount(Boolean onlyTopGroups) {
        return delegate.getGroupsCount(onlyTopGroups);
    }

    @Override
    public Long getGroupsCountByNameContaining(String search) {
        return delegate.getGroupsCountByNameContaining(search);
    }

    @Override
    public List<GroupModel> getTopLevelGroups() {
        return delegate.getTopLevelGroups();
    }

    @Override
    public List<GroupModel> getTopLevelGroups(Integer first, Integer max) {
        return delegate.getTopLevelGroups(first, max);
    }

    @Override
    public List<GroupModel> searchForGroupByName(String search, Integer first, Integer max) {
        return delegate.searchForGroupByName(search, first, max);
    }

    @Override
    public boolean removeGroup(GroupModel group) {
        return delegate.removeGroup(group);
    }

    @Override
    public void moveGroup(GroupModel group, GroupModel toParent) {
        delegate.moveGroup(group, toParent);
    }

    @Override
    public List<ClientScopeModel> getClientScopes() {
        return delegate.getClientScopes();
    }

    @Override
    public ClientScopeModel addClientScope(String name) {
        return delegate.addClientScope(name);
    }

    @Override
    public ClientScopeModel addClientScope(String id, String name) {
        return delegate.addClientScope(id, name);
    }

    @Override
    public boolean removeClientScope(String id) {
        return delegate.removeClientScope(id);
    }

    @Override
    public ClientScopeModel getClientScopeById(String id) {
        return delegate.getClientScopeById(id);
    }

    @Override
    public void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        delegate.addDefaultClientScope(clientScope, defaultScope);
    }

    @Override
    public void removeDefaultClientScope(ClientScopeModel clientScope) {
        delegate.removeDefaultClientScope(clientScope);
    }

    @Override
    public List<ClientScopeModel> getDefaultClientScopes(boolean defaultScope) {
        return delegate.getDefaultClientScopes(defaultScope);
    }
}
