package io.cloudtrust.keycloak.credential;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import org.keycloak.Config;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.credential.PasswordCredentialProviderFactory;
import org.keycloak.models.KeycloakSession;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CtPasswordCredentialProviderFactory extends PasswordCredentialProviderFactory {
    /* START - Copied from parent class, private variables renamed with prefix ct */
    private static final String HASHES_COUNTER_TAGS = "validations-counter-tags";
    private static final String KEYCLOAK_METER_NAME_PREFIX = "keycloak.";
    private static final String LOGIN_PASSWORD_VERIFY_METER_NAME = KEYCLOAK_METER_NAME_PREFIX + "credentials.password.hashing";
    private static final String LOGIN_PASSWORD_VERIFY_METER_DESCRIPTION = "Password validations";
    private static final String HASHES_COUNTER_TAGS_DEFAULT_VALUE = String.format("%s,%s,%s,%s", METER_REALM_TAG, METER_ALGORITHM_TAG, METER_HASHING_STRENGTH_TAG, METER_VALIDATION_OUTCOME_TAG);

    private Meter.MeterProvider<Counter> ctMeterProvider;
    private boolean ctMetricsEnabled;
    private boolean ctWithRealmInMetric;
    private boolean ctWithAlgorithmInMetric;
    private boolean ctWithHashingStrengthInMetric;
    private boolean ctWithOutcomeInMetric;
    /* END - Copied from parent class */

    @Override
    public PasswordCredentialProvider create(KeycloakSession session) {
        return new CtPasswordCredentialProvider(session, ctMeterProvider, ctMetricsEnabled, ctWithRealmInMetric, ctWithAlgorithmInMetric, ctWithHashingStrengthInMetric, ctWithOutcomeInMetric);
    }

    /* START - Copied from parent class */
    @Override
    public void init(Config.Scope config) {
        ctMetricsEnabled = config.getBoolean("metrics-enabled", false);
        if (ctMetricsEnabled) {
            ctMeterProvider = Counter.builder(LOGIN_PASSWORD_VERIFY_METER_NAME)
                    .description(LOGIN_PASSWORD_VERIFY_METER_DESCRIPTION)
                    .baseUnit("validations")
                    .withRegistry(Metrics.globalRegistry);

            Set<String> tags = Arrays.stream(config.get(HASHES_COUNTER_TAGS, HASHES_COUNTER_TAGS_DEFAULT_VALUE).split(",")).collect(Collectors.toSet());
            ctWithRealmInMetric = tags.contains(METER_REALM_TAG);
            ctWithAlgorithmInMetric = tags.contains(METER_ALGORITHM_TAG);
            ctWithHashingStrengthInMetric = tags.contains(METER_HASHING_STRENGTH_TAG);
            ctWithOutcomeInMetric = tags.contains(METER_VALIDATION_OUTCOME_TAG);
        }
    }
    /* END - Copied from parent class */
}
