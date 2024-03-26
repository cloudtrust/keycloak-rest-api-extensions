package io.cloudtrust.keycloak.email.model;

import org.junit.jupiter.api.Test;

import io.cloudtrust.tests.GetterSetterVerifier;

class EmailModelTest {
    @Test
    void getSetTest() {
        GetterSetterVerifier.forClass(EmailModel.class).usesDefaultConstructors().verify();
    }
}
