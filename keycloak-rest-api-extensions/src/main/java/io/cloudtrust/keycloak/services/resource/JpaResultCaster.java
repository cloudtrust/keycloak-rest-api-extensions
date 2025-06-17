package io.cloudtrust.keycloak.services.resource;

import io.cloudtrust.exception.CloudtrustRuntimeException;

import java.math.BigInteger;

public class JpaResultCaster {
    private JpaResultCaster() {
        // Don't instanciate
    }

    public static String toString(Object o) {
        if (o == null) {
            return null;
        }
        return o.toString();
    }

    public static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        switch (o) {
            case BigInteger bi:
                return bi.longValue();
            case Long l:
                return l;
            case Integer i:
                return i.longValue();
            default:
                throw new CloudtrustRuntimeException("Can't convert " + o + " to Long (type is " + o.getClass().getName() + ")");
        }
    }
}
