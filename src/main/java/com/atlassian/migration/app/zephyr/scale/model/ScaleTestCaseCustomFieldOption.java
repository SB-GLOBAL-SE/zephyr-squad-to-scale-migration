package com.atlassian.migration.app.zephyr.scale.model;

public record ScaleTestCaseCustomFieldOption(
        String name,
        int index,
        boolean archived
) {
}
