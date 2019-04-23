# keycloak-rest-api-extensions

This module implementents extensions to keycloak's REST API. Currently implemented functions are:

* admin
  * gets a list of users credentials
  * delete a credential
  * gets users filtered by group or by role
* account : Fix a bug with CORS

Currently works under keycloak 4.8.3.Final

## Installation

Requires java 8 and maven 3.X

```
mvn clean package
./install.sh <KEYCLOAK_HOME>
```

## Usage

To call the REST first get an access token as described in 
[Keycloak's documentation](https://www.keycloak.org/docs/latest/server_development/index.html#example-using-curl).

### Credentials

To get the credentials:
```
curl \
  -H "Authorization: bearer eyJhbGciOiJSUz..." \
  "http://localhost:8080/auth/realms/master/api/admin/realms/{realm}/users/{userid}/credentials"
```

If you want to delete a credential:
```
curl \
  -H "Authorization: bearer eyJhbGciOiJSUz..." \
  -v -X "DELETE"
  "http://localhost:8080/auth/realms/master/api/admin/realms/{realm}/users/{userid}/credentials/{credentialid}"
```

### Users filtered by group or by role

To get a list of users filtered by group id:
```
curl \
  -H "Authorization: bearer eyJhbGciOiJSUz..." \
  "http://localhost:8080/auth/realms/master/api/admin/realms/{realm}/users?groupId=group1
```

Note: it must be the group id, not the name. It is also possible to filter by multiple groups, for example with 
`.../users?groupId=group1&groupId=group2`. This will return the union of all users in `group1` and all users in `group2`


To get a list of users filtered by role name
```
curl \
  -H "Authorization: bearer eyJhbGciOiJSUz..." \
  "http://localhost:8080/auth/realms/master/api/admin/realms/{realm}/users?roleName=role1
```

It is also possible to filter by multiple roles, for example with `.../users?roleName=role1&roleName=role2`. 
This will return the union of all users with `role1` and all users with `role2`

It is also possible to combine the two: 
```
curl \
  -H "Authorization: bearer eyJhbGciOiJSUz..." \
  "http://localhost:8080/auth/realms/master/api/admin/realms/{realm}/users?roleName=role1&roleName=role2&groupId=group1&groupId=group2
```
This will search the intersection of users with groups `group1` or `group2` and of users with roles `role1` and `role2`