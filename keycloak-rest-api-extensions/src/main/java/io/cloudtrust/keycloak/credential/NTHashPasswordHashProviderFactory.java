package io.cloudtrust.keycloak.credential;

public class NTHashPasswordHashProviderFactory extends AbstractSshaPasswordHashProviderFactory<NTHashPasswordHashProvider> {
    public static final String ID = "nthash";

    @Override
    protected NTHashPasswordHashProvider createProvider() {
        return new NTHashPasswordHashProvider();
    }

    @Override
    public String getId() {
        return ID;
    }
}
