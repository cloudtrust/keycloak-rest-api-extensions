package io.cloudtrust.keycloak.services.api.idp;

import io.cloudtrust.keycloak.AbstractRestApiExtensionTest;
import io.cloudtrust.keycloak.config.ServerConfig;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = ServerConfig.class)
class CtIdpRealmResourceTest extends AbstractRestApiExtensionTest {

    private static final String EXTIDP_TEAMMEMBER_GROUP = "extidp_teammember";
    private static final String IDP_ALIAS = "test-idp";
    private static final String DELETE_TEAM_MEMBER_PATH_FMT = "/realms/master/api/idp/realms/test/team-members/%s";

    @BeforeEach
    public void initToken() {
        this.api(keycloak, testRealm).initToken();
    }

    private void ensureIdentityProviderExists(RealmResource realmAdmin) {
        try {
            realmAdmin.identityProviders().get(IDP_ALIAS).toRepresentation();
        } catch (Exception e) {
            IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
            idp.setAlias(IDP_ALIAS);
            idp.setProviderId("oidc");
            idp.setEnabled(true);
            Map<String, String> config = new HashMap<>();
            config.put("authorizationUrl", "https://example.com/auth");
            config.put("tokenUrl", "https://example.com/token");
            config.put("clientId", "test-client");
            config.put("clientSecret", "test-secret");
            idp.setConfig(config);
            realmAdmin.identityProviders().create(idp).close();
        }
    }

    @Test
    void deleteExtIdpTeamMemberUserSuccess() throws IOException, URISyntaxException {
        RealmResource realmAdmin = testRealm.admin();

        ensureIdentityProviderExists(realmAdmin);
        createExtIdpTeamMemberGroup(realmAdmin);
        String userId = createUser(testRealm, "idp-team-member-user", u -> {
            u.setEmail("idp-team-member-user@test.com");
            u.setEnabled(true);
            u.setGroups(List.of("/" + EXTIDP_TEAMMEMBER_GROUP));
            u.singleAttribute("parentID", "parent-user-id");
        });

        addFederatedIdentity(realmAdmin, userId, "ext-user-id-idp-team-member-user", "ext-idp-team-member-user");

        String path = String.format(DELETE_TEAM_MEMBER_PATH_FMT, userId);
        api(keycloak, testRealm).call("DELETE", path);

        List<UserRepresentation> users = realmAdmin.users().searchByUsername("idp-team-member-user", true);
        assertThat(users.size(), is(0));
    }

    @Test
    void deleteExtIdpTeamMemberUserNotInGroupFails() throws IOException, URISyntaxException {
        String userId = createUser(testRealm, "not-in-group-user", u -> {
            u.setEmail("not-in-group@test.com");
            u.setEnabled(true);
        });

        String path = String.format(DELETE_TEAM_MEMBER_PATH_FMT, userId);
        try {
            api(keycloak, testRealm).call("DELETE", path);
            Assertions.fail("Expected 400 Bad Request");
        } catch (HttpResponseException hre) {
            assertThat(hre.getStatusCode(), is(400));
        }
    }

    @Test
    void deleteExtIdpTeamMemberUserMissingParentIdFails() throws IOException, URISyntaxException {
        RealmResource realmAdmin = testRealm.admin();

        ensureIdentityProviderExists(realmAdmin);
        createExtIdpTeamMemberGroup(realmAdmin);
        String userId = createUser(testRealm, "no-parent-id-user", u -> {
            u.setEmail("no-parent-id@test.com");
            u.setEnabled(true);
            u.setGroups(List.of("/" + EXTIDP_TEAMMEMBER_GROUP));
        });

        addFederatedIdentity(realmAdmin, userId, "ext-user-id", "ext-username");

        String path = String.format(DELETE_TEAM_MEMBER_PATH_FMT, userId);
        try {
            api(keycloak, testRealm).call("DELETE", path);
            Assertions.fail("Expected 400 Bad Request");
        } catch (HttpResponseException hre) {
            assertThat(hre.getStatusCode(), is(400));
        }
    }

    @Test
    void deleteExtIdpTeamMemberUserMissingFederatedIdentityFails() throws IOException, URISyntaxException {
        RealmResource realmAdmin = testRealm.admin();

        createExtIdpTeamMemberGroup(realmAdmin);
        String userId = createUser(testRealm, "no-fed-identity-user", u -> {
            u.setEmail("no-fed-identity@test.com");
            u.setEnabled(true);
            u.setGroups(List.of("/" + EXTIDP_TEAMMEMBER_GROUP));
            u.singleAttribute("parentID", "parent-user-id");
        });

        String path = String.format(DELETE_TEAM_MEMBER_PATH_FMT, userId);
        try {
            api(keycloak, testRealm).call("DELETE", path);
            Assertions.fail("Expected 400 Bad Request");
        } catch (HttpResponseException hre) {
            assertThat(hre.getStatusCode(), is(400));
        }
    }

    @Test
    void deleteExtIdpTeamMemberUserNotFoundFails() throws IOException, URISyntaxException {
        String path = String.format(DELETE_TEAM_MEMBER_PATH_FMT, "non-existing-user-id");
        try {
            api(keycloak, testRealm).call("DELETE", path);
            Assertions.fail("Expected error response");
        } catch (HttpResponseException hre) {
            assertThat(hre.getStatusCode() >= 400, is(true));
        }
    }

    private void createExtIdpTeamMemberGroup(RealmResource realmAdmin) {
        List<GroupRepresentation> existingGroups = realmAdmin.groups().groups(EXTIDP_TEAMMEMBER_GROUP, null, null);
        if (!existingGroups.isEmpty()) {
            return;
        }
        GroupRepresentation group = new GroupRepresentation();
        group.setName(EXTIDP_TEAMMEMBER_GROUP);
        var response = realmAdmin.groups().add(group);
        response.close();
    }

    private void addFederatedIdentity(RealmResource realmAdmin, String userId, String extUserId, String extUserName) {
        FederatedIdentityRepresentation fidRep = new FederatedIdentityRepresentation();
        fidRep.setIdentityProvider(IDP_ALIAS);
        fidRep.setUserId(extUserId);
        fidRep.setUserName(extUserName);
        try (Response response = realmAdmin.users().get(userId).addFederatedIdentity(IDP_ALIAS, fidRep)) {
            assertThat("Failed to add federated identity: " + response.getStatusInfo(), response.getStatus() / 100, is(2));
        }
        List<FederatedIdentityRepresentation> identities = realmAdmin.users().get(userId).getFederatedIdentity();
        assertThat("Federated identity was not created", identities.isEmpty(), is(false));
    }
}
