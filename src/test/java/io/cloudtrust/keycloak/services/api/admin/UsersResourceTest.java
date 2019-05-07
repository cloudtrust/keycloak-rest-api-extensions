package io.cloudtrust.keycloak.services.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtrust.keycloak.ApiTest;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class UsersResourceTest extends ApiTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testStandardGetUsers() throws IOException, URISyntaxException {
            UserRepresentation[] users = mapper.readValue(callApi("admin/realms/test/users"), UserRepresentation[].class);
            assertThat(users, notNullValue());
            assertThat(users, arrayWithSize(8));
            assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                    arrayContainingInAnyOrder("non-duplicate-email-user", "rolerichuser", "level2groupuser", "topgroupuser",
                            "keycloak-user@localhost", "john-doh@localhost", "test-user@localhost", "topgroupuser2"));
    }

    @Test
    public void testStandardGetUser() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("username", "rolerichuser"));
        UserRepresentation[] users = mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
        assertThat(users, notNullValue());
        assertThat(users, arrayWithSize(1));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("rolerichuser"));
    }

    @Test
    public void testGetUsersWithGroup() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("groupId", testRealm.groups().groups("topGroup", null, null).get(0).getId()));
        UserRepresentation[] users = mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
        assertThat(users, arrayWithSize(2));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("topgroupuser","topgroupuser2"));

        nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("groupId", testRealm.groups().groups("roleRichGroup", null, null).stream()
                .flatMap(group -> group.getSubGroups().stream()).filter(group -> group.getName().equals("level2group")).findFirst().get().getId()));
        users = mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
        assertThat(users, arrayWithSize(1));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("rolerichuser"));

        nvps.add(new BasicNameValuePair("groupId", testRealm.groups().groups("topGroup", null, null).get(0).getId()));
        users = mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
        assertThat(users, arrayWithSize(3));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("topgroupuser","topgroupuser2","rolerichuser"));
    }

    @Test
    public void testGetUsersWithRole() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("roleId", "user"));
        UserRepresentation[] users = mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
        assertThat(users, arrayWithSize(5));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("non-duplicate-email-user","topgroupuser2"
                        ,"keycloak-user@localhost", "john-doh@localhost", "test-user@localhost"));

        nvps.add(new BasicNameValuePair("roleId", "offline_access"));
        users = mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
        assertThat(users, arrayWithSize(6));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("non-duplicate-email-user","topgroupuser2"
                        ,"keycloak-user@localhost", "john-doh@localhost", "test-user@localhost", "topgroupuser"));
    }

    @Test
    public void testGetUsersWithGroupAndRole() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("roleId", "user"));
        nvps.add(new BasicNameValuePair("groupId", testRealm.groups().groups("topGroup", null, null).get(0).getId()));
        UserRepresentation[] users = mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
        assertThat(users, arrayWithSize(1));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("topgroupuser2"));

        nvps.add(new BasicNameValuePair("roleId", "offline_access"));
        users = mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
        assertThat(users, arrayWithSize(2));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("topgroupuser2", "topgroupuser"));
    }

    @Test
    public void testGetUsersWithNonExistingGroup() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("groupId", "123"));
        UserRepresentation[] users = mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
        assertThat(users, arrayWithSize(0));
    }

    @Test
    public void testGetUsersWithNonExistingRole() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("roleId", "123"));
        UserRepresentation[] users = mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
        assertThat(users, arrayWithSize(0));
    }

    @Test
    public void testGestUsersWithGroupAndFirst() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("groupId", testRealm.groups().groups("topGroup", null, null).get(0).getId()));
        nvps.add(new BasicNameValuePair("first", "0"));
        thrown.expect(HttpResponseException.class);
        thrown.expectMessage(containsString("501"));
        mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
    }

    @Test
    public void testGestUsersWithGroupAndMax() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("groupId", testRealm.groups().groups("topGroup", null, null).get(0).getId()));
        nvps.add(new BasicNameValuePair("max", "10"));
        thrown.expect(HttpResponseException.class);
        thrown.expectMessage(containsString("501"));
        mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
    }

    @Test
    public void testGestUsersWithRoleAndFirst() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("roleId", "offline_access"));
        nvps.add(new BasicNameValuePair("first", "0"));
        thrown.expect(HttpResponseException.class);
        thrown.expectMessage(containsString("501"));
        mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
    }

    @Test
    public void testGestUsersWithRoleAndMax() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("roleId", "offline_access"));
        nvps.add(new BasicNameValuePair("max", "10"));
        thrown.expect(HttpResponseException.class);
        thrown.expectMessage(containsString("501"));
        mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
    }

    @Test
    public void testGestUsersWithGroupAndSearch() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("groupId",  testRealm.groups().groups("topGroup", null, null).get(0).getId()));
        nvps.add(new BasicNameValuePair("search", "topgroupuser2"));
        UserRepresentation[] users = mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
        assertThat(users, arrayWithSize(1));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("topgroupuser2"));
    }

    @Test
    public void testGestUsersWithSearch() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("search", "topgroupuser"));
        UserRepresentation[] users = mapper.readValue(callApi("admin/realms/test/users", nvps), UserRepresentation[].class);
        assertThat(users, arrayWithSize(2));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("topgroupuser", "topgroupuser2"));
    }

}
