package io.cloudtrust.keycloak.email.model;

import io.cloudtrust.tests.GetterSetterVerifier;
import org.junit.jupiter.api.Test;

class EmailModelTest {
    @Test
    void getSetTest() {
        GetterSetterVerifier.forClass(EmailModel.class).usesDefaultConstructors().verify();
    }
}
