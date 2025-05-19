package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;
import java.util.Map;

public record ScaleExecutionCreationPayload(
        String status,
        String testCaseKey,
        Object executedBy,
        Object executionDate,
        Object assignedTo,
        Object comment,
        String version,
        List<String> issueLinks,
        List<ScaleExecutionStepPayload> scriptResults,
        Map<String, String> customFields
//        ScaleMigrationExecutionCustomFieldPayload customFields
) {
}