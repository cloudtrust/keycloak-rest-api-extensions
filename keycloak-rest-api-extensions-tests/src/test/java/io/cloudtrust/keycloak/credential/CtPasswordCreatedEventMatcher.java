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
                    isResetPassword(event.getDetails().get(Events.CT_EVENT_TYPE)) &&
                    event.getDetails().containsKey("credentialId") &&
                    StringUtils.isEmpty(event.getError());
        }
        return false;
    }

    private boolean isResetPassword(String detail) {
        return detail.equals(EventType.RESET_PASSWORD.toString());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("password creation event");
    }
}
