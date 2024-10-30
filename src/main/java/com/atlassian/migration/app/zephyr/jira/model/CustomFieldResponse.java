package com.atlassian.migration.app.zephyr.jira.model;

import java.util.ArrayList;
import java.util.List;

public record CustomFieldResponse(
        int maxResults,
        int startAt,
        int total,
        boolean isLast,
        List<CustomField> values) {}
