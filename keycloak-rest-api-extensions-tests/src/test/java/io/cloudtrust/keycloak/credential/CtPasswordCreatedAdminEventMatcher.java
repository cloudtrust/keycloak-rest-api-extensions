package io.cloudtrust.keycloak.credential;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.AdminEventRepresentation;

public class CtPasswordCreatedAdminEventMatcher extends BaseMatcher<AdminEventRepresentation> {

    public static CtPasswordCreatedAdminEventMatcher isPasswordCreatedAdminEvent() {
        return new CtPasswordCreatedAdminEventMatcher();
    }

    @Override
    public boolean matches(Object item) {
        if (item instanceof AdminEventRepresentation event) {
            return event.getOperationType().equals(OperationType.CREATE.toString()) &&
                    event.getResourceType().equals(ResourceType.CUSTOM.toString()) &&
                    StringUtils.isEmpty(event.getError());
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("admin password creation event");
    }
}
