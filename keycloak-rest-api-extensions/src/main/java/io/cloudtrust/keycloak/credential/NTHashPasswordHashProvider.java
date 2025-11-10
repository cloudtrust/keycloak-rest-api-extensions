package io.cloudtrust.keycloak.credential;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.logging.Logger;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.HexFormat;

public class NTHashPasswordHashProvider implements PasswordHashProvider {
    private static final Logger logger = Logger.getLogger(NTHashPasswordHashProvider.class);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
        return policy != null && credential.getPasswordCredentialData().getAlgorithm().equals(NTHashPasswordHashProviderFactory.ID);
    }

    @Override
    public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
        try {
            String ntHash = computeNTHash(rawPassword);
            return PasswordCredentialModel.createFromValues(NTHashPasswordHashProviderFactory.ID, null, 1, ntHash);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            logger.error("Failed to create encoded credential", e);
            return null;
        }
    }

    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credential) {
        try {
            String storedHash = credential.getPasswordSecretData().getValue();
            String computedHash = computeNTHash(rawPassword);
            return storedHash.equals(computedHash);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            logger.error("Failed to verify password", e);
            return false;
        }
    }

    @Override
    public void close() {
        // Nothing to do
    }

    /**
     * Computes NT hash = MD4(UTF-16LE(password))
     */
    private static String computeNTHash(String password) throws NoSuchAlgorithmException, NoSuchProviderException {
        MessageDigest md4 = MessageDigest.getInstance("MD4", "BC");
        byte[] hash = md4.digest(password.getBytes(StandardCharsets.UTF_16LE));
        return HexFormat.of().withUpperCase().formatHex(hash);
    }
}
