package com.atlassian.migration.app.zephyr.scale.model;

public record ScaleCycleTraceLinkPayload(
        int testRunId,
        String issueId,
        int typeId
) {
}
