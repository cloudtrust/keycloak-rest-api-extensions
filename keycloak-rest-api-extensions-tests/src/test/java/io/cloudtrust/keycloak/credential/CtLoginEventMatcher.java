package io.cloudtrust.keycloak.credential;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;

public class CtLoginEventMatcher extends BaseMatcher<EventRepresentation> {

    public static CtLoginEventMatcher isLoginEvent() {
        return new CtLoginEventMatcher();
    }

    @Override
    public boolean matches(Object item) {
        if (item instanceof EventRepresentation event) {
            return event.getType().equals(EventType.LOGIN.toString()) &&
                    StringUtils.isEmpty(event.getError());
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("login creation event");
    }
}
