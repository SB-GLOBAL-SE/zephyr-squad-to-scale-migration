package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;

public record ScaleExecutionCreationPayload(
        String status,
        String testCaseKey,
        Object executedBy,
        Object comment,
        String version,

        List<String> issueLinks,
        ScaleMigrationExecutionCustomFieldPayload customFields) {
}