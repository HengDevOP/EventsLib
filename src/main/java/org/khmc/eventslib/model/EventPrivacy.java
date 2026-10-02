package org.khmc.eventslib.model;

import java.util.Locale;

public enum EventPrivacy {
    PRIVATE("&cPrivate"),
    PUBLIC("&aPublic");

    private final String formattedDisplay;

    EventPrivacy(String formattedDisplay) {
        this.formattedDisplay = formattedDisplay;
    }

    public String getFormattedDisplay() {
        return formattedDisplay;
    }

    public boolean isPrivate() {
        return this == PRIVATE;
    }

    public boolean isPublic() {
        return this == PUBLIC;
    }

    public static EventPrivacy fromString(String input) {
        if (input == null) return PRIVATE;
        String clean = input.trim().toLowerCase(Locale.ROOT);
        if (clean.equals("public") || clean.equals("pub")) {
            return PUBLIC;
        }
        return PRIVATE;
    }
}
