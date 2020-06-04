package io.cloudtrust.keycloak.services;

import org.jboss.resteasy.spi.LoggableFailure;

public class NotImplementedException extends LoggableFailure {
    private static final long serialVersionUID = -4358044484965118889L;

    public NotImplementedException(){
        super(501);
    }

    public NotImplementedException(String s){
        super(s, 501);
    }
}
