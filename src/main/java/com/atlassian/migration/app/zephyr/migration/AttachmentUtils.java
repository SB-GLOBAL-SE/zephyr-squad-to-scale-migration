package com.atlassian.migration.app.zephyr.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AttachmentUtils {

    private static final Logger logger = LoggerFactory.getLogger(AttachmentUtils.class);

    public static String getYearMonthSubDir(String createdOn) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(createdOn, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return dateTime.getYear() + "/" + String.format("%02d", dateTime.getMonthValue());
        } catch (Exception e) {
            logger.warn("Could not parse createdOn date: " + createdOn + ". Using 'unknown' directory.", e);
            return "unknown";
        }
    }
}
