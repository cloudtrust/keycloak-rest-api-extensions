package io.cloudtrust.keycloak.services;

import io.cloudtrust.exception.CloudtrustRuntimeException;
import io.cloudtrust.keycloak.services.resource.JpaResultCaster;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class JpaResultCasterTest {
    @Test
    void testToString() {
        assertThat(JpaResultCaster.toString(null), nullValue());
        assertThat(JpaResultCaster.toString(5), is("5"));
        assertThat(JpaResultCaster.toString("abc"), is("abc"));
    }

    @Test
    void testToLong() {
        assertThat(JpaResultCaster.toLong(null), nullValue());
        assertThat(JpaResultCaster.toLong(BigInteger.valueOf(74)), is(74L));
        assertThat(JpaResultCaster.toLong(Long.valueOf(51)), is(51L));
        assertThat(JpaResultCaster.toLong(Integer.valueOf(69)), is(69L));
    }

    @Test
    void testToLongFails() {
        CloudtrustRuntimeException cre = Assertions.assertThrows(CloudtrustRuntimeException.class, () -> JpaResultCaster.toLong(System.out));
        assertThat(cre.getMessage(), containsString("Can't convert"));
        assertThat(cre.getMessage(), containsString("PrintStream"));
    }
}
