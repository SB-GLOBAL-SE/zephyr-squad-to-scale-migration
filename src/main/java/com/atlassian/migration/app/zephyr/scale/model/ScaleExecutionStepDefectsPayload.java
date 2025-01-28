package com.atlassian.migration.app.zephyr.scale.model;

public record ScaleExecutionStepDefectsPayload(
        int testResultId,
        int testScriptResultId,
        int typeId,
        String issueId
) {
}
