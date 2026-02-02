package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;
import java.util.Map;

public record FetchScaleTestCasePayload(
        int id,
        String name,
        String key
) {
}