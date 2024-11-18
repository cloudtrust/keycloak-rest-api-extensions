package io.cloudtrust.keycloak.credential;

import io.cloudtrust.keycloak.Events;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;

public class CtPasswordCreatedEventMatcher extends BaseMatcher<EventRepresentation> {

    public static CtPasswordCreatedEventMatcher isPasswordCreatedEvent() {
        return new CtPasswordCreatedEventMatcher();
    }

    @Override
    public boolean matches(Object item) {
        if (item instanceof EventRepresentation event) {
            return event.getType().equals(EventType.CUSTOM_REQUIRED_ACTION.toString()) &&
                    event.getDetails().get(Events.CT_EVENT_TYPE).equals(EventType.UPDATE_PASSWORD.toString()) &&
                    event.getDetails().containsKey("credentialId") &&
                    StringUtils.isEmpty(event.getError());
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("password creation event");
    }
}
