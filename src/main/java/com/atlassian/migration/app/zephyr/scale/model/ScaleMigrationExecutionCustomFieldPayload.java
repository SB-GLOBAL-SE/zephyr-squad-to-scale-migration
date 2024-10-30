package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;
import java.util.Map;

import static com.atlassian.migration.app.zephyr.scale.model.ScaleCustomFieldPayload.TYPE_SINGLE_LINE_TEXT;

public record ScaleMigrationExecutionCustomFieldPayload(
        Object executedOn,
        Object assignedTo,
        Object squadVersion,
        String squadCycleName,
        String folderName
) {
    public static final List<String> CUSTOM_FIELDS_NAMES = List.of("executedOn", "assignedTo", "squadVersion",
            "squadCycleName", "folderName");

    public static final String ENTITY_TYPE = "TEST_EXECUTION";

    public static final Map<String, String> CUSTOM_FIELD_TO_TYPE = Map.of(
            "executedOn", TYPE_SINGLE_LINE_TEXT,
            "assignedTo", TYPE_SINGLE_LINE_TEXT,
            "squadVersion", TYPE_SINGLE_LINE_TEXT,
            "squadCycleName", TYPE_SINGLE_LINE_TEXT,
            "folderName", TYPE_SINGLE_LINE_TEXT
    );
}