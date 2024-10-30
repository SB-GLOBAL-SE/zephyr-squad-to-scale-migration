package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;
import java.util.Map;

public record ScaleTestCaseCreationPayload(
        String projectKey,
        String name,
        String objective,
        List<String> labels,
        String owner,
        List<String> issueLinks,
        Map<String, Object> customFields
) {
}