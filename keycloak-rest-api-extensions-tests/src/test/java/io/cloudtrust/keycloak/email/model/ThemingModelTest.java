package io.cloudtrust.keycloak.email.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.cloudtrust.tests.GetterSetterVerifier;

class ThemingModelTest {
    @Test
    void getSetTest() {
        GetterSetterVerifier.forClass(ThemingModel.class).usesDefaultConstructors()
            .usesConstructor(List.class, () -> new ArrayList<String>())
            .usesConstructor(Map.class, () -> new HashMap<String, String>())
            .verify();
    }

    @Test
    void getSubjectParametersAsArrayTest() {
        // Null subject parameters
        ThemingModel model = new ThemingModel();
        Assertions.assertNull(model.getSubjectParameters());
        Assertions.assertEquals(model.getSubjectParametersAsArray().length, 0);
        // Empty subject parameters
        model.setSubjectParameters(new ArrayList<>());
        Assertions.assertEquals(0, model.getSubjectParameters().size());
        Assertions.assertEquals(0, model.getSubjectParametersAsArray().length);
        // Non-empty subject parameters
        model.getSubjectParameters().add("one");
        Assertions.assertEquals(1, model.getSubjectParameters().size());
        Assertions.assertEquals(1, model.getSubjectParametersAsArray().length);
    }
}
