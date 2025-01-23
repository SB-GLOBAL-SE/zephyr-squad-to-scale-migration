package com.atlassian.migration.app.zephyr.scale.model;

public record ScaleExecutionStepPayload(
        int index,
        String status,
        String comment
) {
}
