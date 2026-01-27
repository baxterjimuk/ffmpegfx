package com.ahthek;
import java.time.Duration;

public class Sekarang {

    // Utility method to format Duration as HH:mm:ss.SSS
    public static String formatDuration(Duration duration) {
        long millis = duration.toMillis();

        long hours = millis / (1000 * 60 * 60);
        long minutes = (millis / (1000 * 60)) % 60;
        long seconds = (millis / 1000) % 60;
        long ms = millis % 1000;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, ms);
    }

    public static void main(String[] args) {
        // Example usage
        Duration d = Duration.between(
                java.time.LocalTime.parse("00:04:19.823"),
                java.time.LocalTime.parse("00:12:23.467")
        );

        System.out.println("Duration: " + formatDuration(d));
    }
}
