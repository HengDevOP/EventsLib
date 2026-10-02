package org.khmc.eventslib.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class TimeResetUtil {

    public static final ZoneId SINGAPORE_ZONE = ZoneId.of("Asia/Singapore");
    public static final LocalTime RESET_TIME = LocalTime.of(15, 0); // 3:00 PM SGT

    private TimeResetUtil() {}

    /**
     * Gets the current cycle key for daily logins resetting at 3:00 PM Singapore time (UTC+8).
     * If current SGT is before 15:00, the cycle belongs to yesterday's 15:00 to today's 14:59:59.
     * If current SGT is at or after 15:00, the cycle belongs to today's 15:00 to tomorrow's 14:59:59.
     */
    public static String getCurrentCycleKey() {
        ZonedDateTime nowSgt = ZonedDateTime.now(SINGAPORE_ZONE);
        LocalDate cycleDate = nowSgt.toLocalTime().isBefore(RESET_TIME)
                ? nowSgt.toLocalDate().minusDays(1)
                : nowSgt.toLocalDate();
        return cycleDate.toString();
    }

    /**
     * Calculates duration until the next 3:00 PM Singapore time.
     */
    public static Duration getTimeUntilNextReset() {
        ZonedDateTime nowSgt = ZonedDateTime.now(SINGAPORE_ZONE);
        ZonedDateTime nextReset;
        if (nowSgt.toLocalTime().isBefore(RESET_TIME)) {
            nextReset = nowSgt.with(RESET_TIME).truncatedTo(ChronoUnit.SECONDS);
        } else {
            nextReset = nowSgt.plusDays(1).with(RESET_TIME).truncatedTo(ChronoUnit.SECONDS);
        }
        return Duration.between(nowSgt, nextReset);
    }

    /**
     * Formats a duration into a human-readable string (e.g., "14h 22m 05s" or "45m 12s").
     */
    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "0s";
        }
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}
