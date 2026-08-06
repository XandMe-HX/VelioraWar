package id.veliora.war.util;

public final class TimeUtil {
    private TimeUtil() {}

    public static String formatSeconds(long totalSeconds) {
        long seconds = Math.max(0, totalSeconds);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remaining = seconds % 60;
        if (hours > 0) return hours + "j " + minutes + "m " + remaining + "d";
        if (minutes > 0) return minutes + "m " + remaining + "d";
        return remaining + "d";
    }

    public static String formatMillis(long millis) {
        return formatSeconds((long) Math.ceil(Math.max(0, millis) / 1000.0));
    }
}
