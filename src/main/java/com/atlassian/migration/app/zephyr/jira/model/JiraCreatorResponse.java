package com.atlassian.migration.app.zephyr.jira.model;

public record JiraCreatorResponse(
    int id,
    String key,
    String name
) { }