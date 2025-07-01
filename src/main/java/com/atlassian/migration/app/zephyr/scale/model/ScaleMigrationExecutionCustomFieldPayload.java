package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;
import java.util.Map;

import static com.atlassian.migration.app.zephyr.scale.model.ScaleCustomFieldPayload.TYPE_SINGLE_LINE_TEXT;

public record ScaleMigrationExecutionCustomFieldPayload(
        Object executedOn,
        Object assignedTo,
        Object squadVersion,
        String squadCycleName,
        String folderName,

        Map<String, String> additionalProperties
) {
    public static final String EXECUTED_ON = "executedOn";
    public static final String ASSIGNED_TO = "assignedTo";
    public static final String SQUAD_VERSION = "squadVersion";
    public static final String SQUAD_CYCLE_NAME = "squadCycleName";
    public static final String FOLDER_NAME = "folderName";
    public static final List<String> CUSTOM_FIELDS_NAMES = List.of(EXECUTED_ON, ASSIGNED_TO, SQUAD_VERSION,
            SQUAD_CYCLE_NAME, FOLDER_NAME);


    public static final String ENTITY_TYPE = "TEST_EXECUTION";

    public static final Map<String, String> CUSTOM_FIELD_TO_TYPE = Map.of(
            EXECUTED_ON, TYPE_SINGLE_LINE_TEXT,
            ASSIGNED_TO, TYPE_SINGLE_LINE_TEXT,
            SQUAD_VERSION, TYPE_SINGLE_LINE_TEXT,
            SQUAD_CYCLE_NAME, TYPE_SINGLE_LINE_TEXT,
            FOLDER_NAME, TYPE_SINGLE_LINE_TEXT
    );
}