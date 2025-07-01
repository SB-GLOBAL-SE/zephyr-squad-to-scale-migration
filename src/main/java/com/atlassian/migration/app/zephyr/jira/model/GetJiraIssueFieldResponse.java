package com.atlassian.migration.app.zephyr.jira.model;

import java.util.List;

public record GetJiraIssueFieldResponse(
        String fieldId,
        String name,
        boolean required,
        IssueTypeSchema schema,
        List<JiraCustomFieldOptionValue> allowedValues

) { }