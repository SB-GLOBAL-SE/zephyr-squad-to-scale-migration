package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;

public record ScaleTestCaseCustomField(
        Integer id,
        String name,
        String type,
        String description,
        boolean archived,
        List<ScaleTestCaseCustomFieldOption> options
) {}
