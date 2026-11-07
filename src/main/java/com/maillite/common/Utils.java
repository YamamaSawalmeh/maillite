package com.maillite.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
    
    public static String formatTime(LocalDateTime time) {
        return time.format(FORMATTER);
    }
}
