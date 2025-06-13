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
 * Implementation of the Salted SHA1 password hash algorithm.
 */
public class Ssha1PasswordHashProvider implements PasswordHashProvider {
    private static final String SSHA1_PREFIX = "{SSHA1}";
    private static final int SSHA1_BYTE_LENGTH = 20;

    @Override
    public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
        return policy != null && credential.getPasswordCredentialData().getAlgorithm().equals(Ssha1PasswordHashProviderFactory.ID);
    }

    @Override
    public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
        byte[] salt = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        byte[] toEncode;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(Bytes.concat(rawPassword.getBytes(), salt));

            toEncode = Bytes.concat(hash, salt);
            String value = SSHA1_PREFIX + Base64.encodeBytes(toEncode);

            return PasswordCredentialModel.createFromValues(Ssha1PasswordHashProviderFactory.ID, salt, 1, value);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credential) {
        if (!credential.getPasswordSecretData().getValue().startsWith(SSHA1_PREFIX)) {
            // String is not properly formatted
            return false;
        }

        try {
            byte[] decodedValue = Base64.decode(credential.getPasswordSecretData().getValue().substring(SSHA1_PREFIX.length()));

            // For verification, the salt contained in the PasswordSecretData object is ignored, we only retrieve it
            // from the value
            byte[] salt = Arrays.copyOfRange(decodedValue, SSHA1_BYTE_LENGTH, decodedValue.length);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(Bytes.concat(rawPassword.getBytes(), salt));

            byte[] result = Bytes.concat(hash, salt);
            return Arrays.equals(result, decodedValue);
        } catch (IOException | NoSuchAlgorithmException e) {
            return false;
        }
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
