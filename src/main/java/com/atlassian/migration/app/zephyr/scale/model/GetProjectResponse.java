package com.atlassian.migration.app.zephyr.scale.model;

import com.atlassian.migration.app.zephyr.jira.model.IssueType;

import java.util.List;

public record GetProjectResponse(
        String key,
        String id,
        List<String> projectKeys,
        List<IssueType> issueTypes
) {
}

