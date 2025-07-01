package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;

public record FetchScaleTestResults(
        int id,
        String key,
        List<ScaleTestScriptResults> testScriptResults
) {
}
