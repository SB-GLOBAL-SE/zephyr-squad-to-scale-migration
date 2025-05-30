package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;
import java.util.Map;

import static com.atlassian.migration.app.zephyr.scale.model.ScaleCustomFieldPayload.TYPE_SINGLE_LINE_TEXT;

public record ScaleMigrationTestStepCustomFieldPayload(

) {
    public static final String ENTITY_TYPE = "TEST_STEP";

}

