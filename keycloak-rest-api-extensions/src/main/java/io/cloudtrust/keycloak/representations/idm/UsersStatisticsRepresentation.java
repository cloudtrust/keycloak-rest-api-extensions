package io.cloudtrust.keycloak.representations.idm;

public class UsersStatisticsRepresentation {

    private long total;
    private long disabled;
    private long inactive;

    /**
     * For unserializing
     */
    protected UsersStatisticsRepresentation() {
    }

    public UsersStatisticsRepresentation(long total, long disabled, long inactive) {
        this.total = total;
        this.disabled = disabled;
        this.inactive = inactive;
    }

    public long getTotal() { return total; }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getDisabled() {
        return disabled;
    }

    public void setDisabled(long disabled) {
        this.disabled = disabled;
    }

    public long getInactive() {
        return inactive;
    }

    public void setInactive(long inactive) {
        this.inactive = inactive;
    }

}
