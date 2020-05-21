package io.cloudtrust.keycloak.services.api.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtrust.keycloak.ApiTest;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class RealmAdminResourceTest extends ApiTest {
    private ObjectMapper mapper = new ObjectMapper();

    private TypeReference<List<String>> lockedUsersTypeRef = new TypeReference<List<String>>() {
    };
    private TypeReference<Map<String, Integer>> usersAuthenticatorsTypeRef = new TypeReference<Map<String, Integer>>() {
    };

    @Test
    public void testGetLockedUsers() throws IOException, URISyntaxException {
        List<String> lockedUsers = mapper.readValue(callApi("admin/realms/test/locked-users"), lockedUsersTypeRef);
        Assert.assertThat(lockedUsers, IsEmptyCollection.empty());
    }

    @Test
    public void testGetAuthenticatorsCount() throws IOException, URISyntaxException {
        Map<String, Integer> authenticators = mapper.readValue(callApi("admin/realms/test/authenticators-count"), usersAuthenticatorsTypeRef);
        // Hard to check user ids as they are always changing
        Assert.assertThat(authenticators, IsMapContaining.hasValue(1));
    }
}
