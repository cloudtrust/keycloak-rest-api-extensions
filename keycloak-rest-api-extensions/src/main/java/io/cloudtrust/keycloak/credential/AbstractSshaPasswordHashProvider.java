package io.cloudtrust.keycloak.credential;

import com.google.common.primitives.Bytes;
import org.keycloak.common.util.Base64;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Abstract base class for Salted SHA password hash algorithms.
 */
public abstract class AbstractSshaPasswordHashProvider implements PasswordHashProvider {
    private final String algorithm;
    private final String prefix;
    private final int hashLength;
    private final String factoryId;

    protected AbstractSshaPasswordHashProvider(String algorithm, String prefix, int hashLength, String factoryId) {
        this.algorithm = algorithm;
        this.prefix = prefix;
        this.hashLength = hashLength;
        this.factoryId = factoryId;
    }

    @Override
    public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
        return policy != null && credential.getPasswordCredentialData().getAlgorithm().equals(factoryId);
    }

    @Override
    public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
        byte[] salt = generateSalt();
        byte[] toEncode;
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(Bytes.concat(rawPassword.getBytes(), salt));

            toEncode = Bytes.concat(hash, salt);
            String value = prefix + Base64.encodeBytes(toEncode);

            return PasswordCredentialModel.createFromValues(factoryId, salt, 1, value);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credential) {
        if (!credential.getPasswordSecretData().getValue().startsWith(prefix)) {
            // String is not properly formatted
            return false;
        }

        try {
            byte[] decodedValue = Base64.decode(credential.getPasswordSecretData().getValue().substring(prefix.length()));

            // For verification, the salt contained in the PasswordSecretData object is ignored, we only retrieve it
            // from the value
            byte[] salt = Arrays.copyOfRange(decodedValue, hashLength, decodedValue.length);
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(Bytes.concat(rawPassword.getBytes(), salt));

            byte[] result = Bytes.concat(hash, salt);
            return Arrays.equals(result, decodedValue);
        } catch (IOException | NoSuchAlgorithmException e) {
            return false;
        }
    }

    protected byte[] generateSalt() {
        byte[] salt = new byte[32];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    @Override
    public void close() {
        // Nothing to do
    }
}



