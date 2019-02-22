# keycloak-rest-api-extensions

This module implementents extensions to keycloak's REST API. Currently implemented functions are:

* credentials: gets a list of users credentials

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

Then use the token to get the credentials:
```
curl \
  -H "Authorization: bearer eyJhbGciOiJSUz..." \
  "http://localhost:8080/auth/realms/{realm}/api/users/{userid}/credentials"
```