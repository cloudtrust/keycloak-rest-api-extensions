package io.cloudtrust.keycloak.services.api.admin;

import io.cloudtrust.keycloak.AbstractRestApiExtensionTest;
import io.cloudtrust.keycloak.representations.idm.UsersPageRepresentation;
import io.cloudtrust.keycloak.test.container.KeycloakDeploy;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(KeycloakDeploy.class)
class CtUsersResourceTest extends AbstractRestApiExtensionTest {
    private static final String getMethod = "GET";

    private String findRoleId(String name) {
        return this.getRealm("test").roles().get(name).toRepresentation().getId();
    }

    private String findGroupId(String name) {
        return this.getRealm("test").groups().groups(name, null, null).get(0).getId();
    }

    @Test
    void testStandardGetUsers() throws IOException, URISyntaxException {
        UsersPageRepresentation page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users");
        UserRepresentation[] users = grabUsers(page);
        assertThat(users, notNullValue());
        assertThat(users, arrayWithSize(8));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("non-duplicate-email-user", "rolerichuser", "level2groupuser", "topgroupuser",
                        "keycloak-user@localhost", "john-doh@localhost", "test-user@localhost", "topgroupuser2"));
        assertThat(page.getCount(), is(8));
    }

    @Test
    void testPaginatedGetUsers() throws IOException, URISyntaxException {
        // The users are sorted alphabetically by username (sorted in GetUsersQuery)
        // Page 1
        List<NameValuePair> nvps = List.of(
                new BasicNameValuePair("first", "0"),
                new BasicNameValuePair("max", "2"));
        UsersPageRepresentation page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        UserRepresentation[] users = grabUsers(page);
        assertThat(users, notNullValue());
        assertThat(users, arrayWithSize(2));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(), arrayContaining("john-doh@localhost", "keycloak-user@localhost"));
        assertThat(page.getCount(), is(8));

        // Page 3
        nvps = List.of(
                new BasicNameValuePair("first", "4"),
                new BasicNameValuePair("max", "2"));
        page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        users = grabUsers(page);
        assertThat(users, notNullValue());
        assertThat(users, arrayWithSize(2));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(), arrayContaining("rolerichuser", "test-user@localhost"));
        assertThat(page.getCount(), is(8));
    }

    @Test
    void testStandardGetUser() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = Collections.singletonList(new BasicNameValuePair("username", "rolerichuser"));
        UsersPageRepresentation page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        UserRepresentation[] users = grabUsers(page);
        assertThat(users, notNullValue());
        assertThat(users, arrayWithSize(1));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("rolerichuser"));
        assertThat(page.getCount(), is(1));
    }

    @ParameterizedTest
    @MethodSource("getUsersWithWildcardSamples")
    void testGetUsersWithWildcard(String field, String value, int expectedCount) throws IOException, URISyntaxException {
        List<NameValuePair> nvps = Collections.singletonList(new BasicNameValuePair(field, value));
        UsersPageRepresentation page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        UserRepresentation[] users = grabUsers(page);
        assertThat(users, notNullValue());
        assertThat(users.length, is(expectedCount));
    }

    public static Stream<Arguments> getUsersWithWildcardSamples() {
        return Stream.of(
                Arguments.of("lastName", "doh", 2),
                Arguments.of("lastName", "%do%", 2),
                Arguments.of("lastName", "doh%", 1),
                Arguments.of("lastName", "%doh", 1),
                Arguments.of("lastName", "=do", 0),
                Arguments.of("lastName", "=doh", 1)
        );
    }

    @Test
    void testGetUsersWithGroup() throws IOException, URISyntaxException {
        RealmResource testRealm = this.getRealm();

        List<NameValuePair> nvps = Collections.singletonList(new BasicNameValuePair("groupId", findGroupId("topGroup")));
        UsersPageRepresentation page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        UserRepresentation[] users = grabUsers(page);
        assertThat(users, arrayWithSize(2));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("topgroupuser", "topgroupuser2"));
        assertThat(page.getCount(), is(2));

        nvps = new ArrayList<>();
        // With KC18, subgroups are not filled in the group when searching with groups().group(...) so we search the group and get it again by its id
        String roleRichGroupId = findGroupId("roleRichGroup");
        GroupRepresentation subGroup = testRealm.groups().group(roleRichGroupId).getSubGroups("level2group", true, 0, 100, false).get(0);
        nvps.add(new BasicNameValuePair("groupId", subGroup.getId()));
        page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        users = grabUsers(page);
        assertThat(users, arrayWithSize(1));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("rolerichuser"));
        assertThat(page.getCount(), is(1));

        nvps.add(new BasicNameValuePair("groupId", findGroupId("topGroup")));
        page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        users = grabUsers(page);
        assertThat(users, arrayWithSize(3));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("topgroupuser", "topgroupuser2", "rolerichuser"));
        assertThat(page.getCount(), is(3));
    }

    @Test
    void testGetUsersWithRole() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("roleId", findRoleId("user")));
        UsersPageRepresentation page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        UserRepresentation[] users = grabUsers(page);
        assertThat(users, arrayWithSize(5));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("non-duplicate-email-user", "topgroupuser2"
                        , "keycloak-user@localhost", "john-doh@localhost", "test-user@localhost"));
        assertThat(page.getCount(), is(5));

        nvps.add(new BasicNameValuePair("roleId", findRoleId("offline_access")));
        page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        users = grabUsers(page);
        assertThat(users, arrayWithSize(6));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("non-duplicate-email-user", "topgroupuser2"
                        , "keycloak-user@localhost", "john-doh@localhost", "test-user@localhost", "topgroupuser"));
        assertThat(page.getCount(), is(6));
    }

    @Test
    void testGetUsersWithGroupAndRole() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("roleId", findRoleId("user")));
        nvps.add(new BasicNameValuePair("groupId", findGroupId("topGroup")));
        UsersPageRepresentation page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        UserRepresentation[] users = grabUsers(page);
        assertThat(users, arrayWithSize(1));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("topgroupuser2"));
        assertThat(page.getCount(), is(1));

        nvps.add(new BasicNameValuePair("roleId", findRoleId("offline_access")));
        page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        users = grabUsers(page);
        assertThat(users, arrayWithSize(2));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("topgroupuser2", "topgroupuser"));
        assertThat(page.getCount(), is(2));
    }

    @Test
    void testGetUsersWithNonExistingGroup() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = Collections.singletonList(new BasicNameValuePair("groupId", "123"));
        UsersPageRepresentation page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        UserRepresentation[] users = grabUsers(page);
        assertThat(users, arrayWithSize(0));
        assertThat(page.getCount(), is(0));
    }

    @Test
    void testGetUsersWithNonExistingRole() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = Collections.singletonList(new BasicNameValuePair("roleId", "123879834564"));
        UsersPageRepresentation page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        UserRepresentation[] users = grabUsers(page);
        assertThat(users, arrayWithSize(0));
        assertThat(page.getCount(), is(0));
    }

    @Test
    void testGetUsersWithGroupAndSearch() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = List.of(
                new BasicNameValuePair("groupId", findGroupId("topGroup")),
                new BasicNameValuePair("search", "topgroupuser2"));
        UsersPageRepresentation page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        UserRepresentation[] users = grabUsers(page);
        assertThat(users, arrayWithSize(1));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("topgroupuser2"));
        assertThat(page.getCount(), is(1));
    }

    @Test
    void testGetUsersWithSearch() throws IOException, URISyntaxException {
        List<NameValuePair> nvps = Collections.singletonList(new BasicNameValuePair("search", "topgroupuser"));
        UsersPageRepresentation page = this.api().query(UsersPageRepresentation.class, getMethod, "/realms/master/api/admin/realms/test/users", nvps);
        UserRepresentation[] users = grabUsers(page);
        assertThat(users, arrayWithSize(2));
        assertThat(Arrays.stream(users).map(UserRepresentation::getUsername).toArray(),
                arrayContainingInAnyOrder("topgroupuser", "topgroupuser2"));
        assertThat(page.getCount(), is(2));
    }

    private UserRepresentation[] grabUsers(UsersPageRepresentation page) {
        return page.getUsers().toArray(n -> new UserRepresentation[n]);
    }
}
