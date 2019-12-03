package io.cloudtrust.keycloak.services;

import org.jboss.resteasy.spi.LoggableFailure;

public class NotImplementedException extends LoggableFailure {

    public NotImplementedException(){
        super(501);
    }

    public NotImplementedException(String s){
        super(s, 501);
    }
}
