package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;

public record ScaleExecutionCreationPayload(
        String status,
        String testCaseKey,
        Object executedBy,
        Object executionDate,
        Object assignedTo,
        Object comment,
        Object version,
        List<String> issueLinks,
        List<ScaleExecutionStepPayload> scriptResults,
        ScaleMigrationExecutionCustomFieldPayload customFields) {
}