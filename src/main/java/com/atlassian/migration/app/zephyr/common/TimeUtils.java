package com.atlassian.migration.app.zephyr.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TimeUtils {

    private static final Logger logger = LoggerFactory.getLogger(TimeUtils.class);
    private static final String DEFAULT_SQUAD_FORMAT = "dd/MMM/yy h:mm a";
    private static final String DEFAULT_JIRA_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String SCALE_TIMEZONE = "UTC";

    public static final String getUTCTimestampforSquadDate(String dateString){
        if(dateString == null){
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_SQUAD_FORMAT, Locale.ENGLISH);
        LocalDateTime localDateTime = LocalDateTime.parse(dateString, formatter);
        ZonedDateTime ldtZoned = localDateTime.atZone(ZoneId.systemDefault());


        ZonedDateTime utcZoned = ldtZoned.withZoneSameInstant(ZoneId.of(SCALE_TIMEZONE));
        return utcZoned.toLocalDateTime().toString();
    }

    public static final String getUTCTimestampforJiraDate(String dateString){
        if(dateString == null){
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_JIRA_FORMAT, Locale.ENGLISH);
            LocalDateTime localDateTime = LocalDateTime.parse(dateString, formatter);
            ZonedDateTime ldtZoned = localDateTime.atZone(ZoneId.systemDefault());


            ZonedDateTime utcZoned = ldtZoned.withZoneSameInstant(ZoneId.of(SCALE_TIMEZONE));
            return utcZoned.toLocalDateTime().toString();
        }catch (Exception e){
            logger.warn("Date parse exception at '"+dateString+"'");
        }
        return dateString;
    }

    public static final String getUTCTImestampforSquadAttachment(String dateString){
        if(dateString == null)
            return null;
        try {
            if (dateString.contains("Yesterday") || dateString.contains("ago")) {
                return parseAttachmentDateformat(dateString);
            }
            return getUTCTimestampforSquadDate(dateString);
        }catch (Exception e){
            logger.warn("Date parse exception at '"+dateString+"'");
        }
        return dateString;
    }

    private static String parseAttachmentDateformat(String dateStr) {
        try {
            String timePart = dateStr.substring(dateStr.indexOf("ago") + 4);
            int daysAgo = 0;
            if (dateStr.contains("Yesterday")) {
                daysAgo = 1;
                timePart = dateStr.substring(dateStr.indexOf(" ") + 1);
            } else if (dateStr.contains("week")) {
                daysAgo = 7;
            } else {
                daysAgo = Integer.parseInt(dateStr.split(" ")[0]);
            }

            // Get the current LocalDateTime
            LocalDateTime now = LocalDateTime.now();

            // Subtract the days
            LocalDateTime targetDate = now.minusDays(daysAgo);

            int add = 0;
            if (timePart.contains("PM"))
                add = 12;
            // Parse the time (9:56 PM) into a LocalTime object
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
            int hour = Integer.parseInt(timePart.split(":")[0]);
            int minutes = Integer.parseInt(timePart.split(":")[1].split(" ")[0]);
            if (hour < 12) {
                hour = hour + add;
            }
            if (timePart.contains("AM") && hour == 12) {
                hour = 0;
            }

            LocalDateTime dateTime = targetDate.withHour(hour)
                    .withMinute(minutes)
                    .withSecond(0);


            ZonedDateTime ldtZoned = dateTime.atZone(ZoneId.systemDefault());
            ZonedDateTime utcZoned = ldtZoned.withZoneSameInstant(ZoneId.of(SCALE_TIMEZONE));
            return utcZoned.toLocalDateTime().toString();
        }catch (Exception e){
            //Some thing went wrong
        }
        return null;
    }
}
