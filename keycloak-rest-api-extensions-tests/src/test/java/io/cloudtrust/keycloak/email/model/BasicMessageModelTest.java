package io.cloudtrust.keycloak.email.model;

import io.cloudtrust.tests.GetterSetterVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BasicMessageModelTest {
    @Test
    void getSetTest() {
        GetterSetterVerifier.forClass(BasicMessageModel.class).usesDefaultConstructors().verify();
    }

    @Test
    void constructorTest() {
        String subject = "subject";
        String txt = "txt";
        String html = "html";
        BasicMessageModel msg = new BasicMessageModel(subject, txt, html);
        Assertions.assertEquals(subject, msg.getSubject());
        Assertions.assertEquals(txt, msg.getTextMessage());
        Assertions.assertEquals(html, msg.getHtmlMessage());
    }
}
