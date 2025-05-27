package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;
import java.util.Map;

import static com.atlassian.migration.app.zephyr.scale.model.ScaleCustomFieldPayload.*;

public record ScaleMigrationTestCaseCustomFieldPayload(
        Object components,
        Object squadStatus,
        Object squadPriority

) {
    public static final List<String> CUSTOM_FIELDS_NAMES = List.of(
//            "components", "squadStatus", "squadPriority"
    );
    public static final String ENTITY_TYPE = "TEST_CASE";

    public static final Map<String, String> MIGRATION_CUSTOM_FIELDS = Map.of(
//            "components", TYPE_SINGLE_LINE_TEXT,
//            "squadStatus", TYPE_SINGLE_LINE_TEXT,
//            "squadPriority", TYPE_SINGLE_LINE_TEXT
    );

    public static final Map<String, String> MIGRATION_JIRA_SCALE_CUSTOM_FIELD_TYPES = Map.of(
            "Select List (single choice)", SINGLE_CHOICE_SELECT_LIST,
            "User Picker (single user)", SINGLE_USER_PICKER
    );


}

