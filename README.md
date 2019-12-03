# keycloak-rest-api-extensions

This module implementents extensions to keycloak's REST API. Currently implemented functions are:

* admin
  * gets a list of users credentials
  * delete a credential
  * gets users filtered by group or by role
  * get user statistics
  * get credentials statistics
* account : Fix a bug with CORS

Currently works under keycloak 7.0.0 multi-token fork

## Installation

Requires java 8 and maven 3.X

```
mvn clean package
keycloak-rest-api-extensions/install.sh <KEYCLOAK_HOME>
```

Add the following to the launch parameters of Keycloak:
```
-Dkeycloak.profile.feature.account_api=enabled
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


To get a list of users filtered by role id
```
curl \
  -H "Authorization: bearer eyJhbGciOiJSUz..." \
  "http://localhost:8080/auth/realms/master/api/admin/realms/{realm}/users?roleId=role1
```

It is also possible to filter by multiple roles, for example with `.../users?roleId=role1&roleId=role2`. 
This will return the union of all users with `role1` and all users with `role2`

It is also possible to combine the two: 
```
curl \
  -H "Authorization: bearer eyJhbGciOiJSUz..." \
  "http://localhost:8080/auth/realms/master/api/admin/realms/{realm}/users?roleId=role1&roleId=role2&groupId=group1&groupId=group2
```
This will search the intersection of users with groups `group1` or `group2` and of users with roles `role1` and `role2`

### User creation with Groups and Roles

To create a user with a specific group and role
```
curl \
  -H "Authorization: bearer eyJhbGciOiJSUz..." \
  -H "Content-Type: application/json" \
  -X POST \
  -d '{ "email": "toto@toto.com", "username": "toto", "realmRoles": ["010b904b-f052-4f4c-a3f1-4b14da3a3448"], "groups": ["dc1689ff-ece8-4b34-bc31-66ea9b254290", "c322b499-0e32-4d42-a76d-a832b4fbb2f9"]}' \
   http://localhost:8080/auth/realms/master/api/admin/realms/master/users
```

The user will be created with roles and groups assigned.

### Statistics

To get the users statistics
```
curl \
  -H "Authorization: bearer eyJhbGciOiJSUz..." \
  "http://localhost:8080/auth/realms/master/api/admin/realms/{realm}/statistics/users
```

To get the credentials statistics
```
curl \
  -H "Authorization: bearer eyJhbGciOiJSUz..." \
  "http://localhost:8080/auth/realms/master/api/admin/realms/{realm}/statistics/credentials
```
