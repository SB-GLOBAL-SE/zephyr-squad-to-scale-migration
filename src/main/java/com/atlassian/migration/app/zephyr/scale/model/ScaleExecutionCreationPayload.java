package com.atlassian.migration.app.zephyr.scale.model;

public record ScaleExecutionCreationPayload(
        String status,
        String testCaseKey,
        Object executedBy,
        Object comment,
        String version,
        ScaleMigrationExecutionCustomFieldPayload customFields) {
}