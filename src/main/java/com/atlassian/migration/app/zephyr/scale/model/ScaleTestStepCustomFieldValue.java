package com.atlassian.migration.app.zephyr.scale.model;

public record ScaleTestStepCustomFieldValue(
        String stringValue,
        Integer intValue,
        String dateValue,
        ScaleCustomFieldResponse customField
) {}
