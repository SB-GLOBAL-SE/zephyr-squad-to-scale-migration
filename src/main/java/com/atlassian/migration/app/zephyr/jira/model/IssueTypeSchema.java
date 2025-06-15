package com.atlassian.migration.app.zephyr.jira.model;

public record IssueTypeSchema(
    String type,
    String custom,
    String customId,
    String system
) {}