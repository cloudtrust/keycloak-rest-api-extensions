package io.cloudtrust.keycloak.representations.idm;

public class UsersStatisticsRepresentation {

    private int total;
    private int inactive;
    private int blocked;

    /**
     * For unserializing
     */
    protected UsersStatisticsRepresentation() {
    }

    public UsersStatisticsRepresentation(int total, int inactive, int blocked) {
        this.total = total;
        this.inactive = inactive;
        this.blocked = blocked;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getInactive() {
        return inactive;
    }

    public void setInactive(int inactive) {
        this.inactive = inactive;
    }

    public int getBlocked() {
        return blocked;
    }

    public void setBlocked(int blocked) {
        this.blocked = blocked;
    }
}
