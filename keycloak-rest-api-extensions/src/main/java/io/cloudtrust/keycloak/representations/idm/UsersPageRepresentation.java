package io.cloudtrust.keycloak.representations.idm;

import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public class UsersPageRepresentation {

    /**
     * Users
     */
    List<UserRepresentation> users;

    /**
     * Total users count
     */
    int count;

    /**
     * For unserializing
     */
    protected UsersPageRepresentation() {
    }

    public UsersPageRepresentation(List<UserRepresentation> users, int count) {
        this.users = users;
        this.count = count;
    }

    public List<UserRepresentation> getUsers() {
        return users;
    }

    public void setUsers(List<UserRepresentation> users) {
        this.users = users;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
