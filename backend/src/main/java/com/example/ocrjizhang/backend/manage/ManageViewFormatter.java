package com.example.ocrjizhang.backend.manage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("manageFormat")
public class ManageViewFormatter {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String money(long balanceFen) {
        return String.format(Locale.CHINA, "￥%,.2f", balanceFen / 100.0d);
    }

    public String dateTime(long timestamp) {
        if (timestamp <= 0L) {
            return "-";
        }
        return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp).atZone(ZONE_ID));
    }

    public String date(long timestamp) {
        if (timestamp <= 0L) {
            return "-";
        }
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timestamp).atZone(ZONE_ID));
    }
}
