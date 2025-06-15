package com.atlassian.migration.app.zephyr.jira.model;

import java.util.List;

public record FetchJiraIssueTypesResponse(
        int startAt,
        int total,
        List<GetJiraIssueFieldResponse> values
) { }