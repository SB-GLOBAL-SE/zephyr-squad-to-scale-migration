package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;

public record ScaleTestCaseCustomField(
        String name,
        String type,
        List<ScaleTestCaseCustomFieldOption> options
) {}
