package io.cloudtrust.keycloak.email.model;

import org.junit.jupiter.api.Test;

import io.cloudtrust.tests.GetterSetterVerifier;

class AttachmentModelTest {
    @Test
    void getSetTest() {
        GetterSetterVerifier.forClass(AttachmentModel.class).usesDefaultConstructors().verify();
    }
}
