package com.atlassian.migration.app.zephyr.scale.model;

public record ScaleExecutionVersionPayload(
        int id,
        String jiraVersionId
) {
}
