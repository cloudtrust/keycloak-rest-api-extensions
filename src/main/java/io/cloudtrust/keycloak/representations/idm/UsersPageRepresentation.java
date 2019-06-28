package io.cloudtrust.keycloak.representations.idm;

import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public class UsersPageRepresentation {

    List<UserRepresentation> users;
    int count;

    public UsersPageRepresentation(List<UserRepresentation> users, int count) {
        this.users = users;
        this.count = count;
    }

    public List<UserRepresentation> getUsers() {
        return users;
    }

    public int getCount() {
        return count;
    }
}
